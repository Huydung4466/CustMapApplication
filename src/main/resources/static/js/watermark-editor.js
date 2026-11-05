(() => {
    'use strict';

    const byId = id => document.getElementById('wm' + id);
    const fileInput = byId('File');
    const originalInput = byId('Original');
    const canvas = byId('Canvas');
    const context = canvas.getContext('2d');
    const names = ['Date', 'Gps', 'Address', 'Code'];
    const fields = names.map(byId);
    const zoom = byId('ZoomCanvas');
    let image, original, pairAligned = false, region, drag, selection, filename;
    let recognized = [];
    let layout = [];
    let selectionForScan;
    let busy = false;
    let edited = false;
    let selecting = false;

    function status(message) {
        byId('Status').textContent = message;
    }

    function sync() {
        ['Before', 'After', 'Select', 'Scan', 'Fields', 'Download'].forEach(id => {
            byId(id).disabled = !image || busy;
        });
        originalInput.disabled = fileInput.disabled = busy;
        byId('Zoom').disabled = !image || busy;
        byId('Style').disabled = !pairReady() || busy;
        byId('Size').disabled = !pairReady() || busy;
        byId('OffsetX').disabled = byId('OffsetY').disabled = !pairReady() || busy;
        names.forEach((name, index) => {
            const message = !layout[index] ? 'Chưa định vị được dòng'
                : !fields[index].value.trim() ? 'Đã định vị · cần nhập nội dung'
                : fields[index].value.trim() === recognized[index] ? 'OCR · cần kiểm tra' : 'Đã nhập hoặc chỉnh tay';
            byId(name + 'State').textContent = message;
        });
        byId('Scan').disabled = byId('Select').disabled = !pairReady() || busy;
        byId('After').disabled = byId('Download').disabled = busy || !pairReady()
            || !fields.every((field, index) => layout[index] && field.value.trim());
        byId('Before').setAttribute('aria-pressed', String(!edited));
        byId('After').setAttribute('aria-pressed', String(edited));
        canvas.classList.toggle('wm-selecting', selecting);
    }

    function pairReady() {
        return image && original && pairAligned;
    }

    function matchesFrame() {
        if (image.width !== original.width || image.height !== original.height) return false;
        const sample = document.createElement('canvas');
        sample.width = 160;
        sample.height = 120;
        const ctx = sample.getContext('2d', { willReadFrequently: true });
        const top = Math.max(1, Math.floor(image.height * 0.76));
        function pixels(bitmap) {
            ctx.clearRect(0, 0, sample.width, sample.height);
            ctx.drawImage(bitmap, 0, 0, image.width, top, 0, 0, sample.width, sample.height);
            return ctx.getImageData(0, 0, sample.width, sample.height).data;
        }
        const reference = pixels(image);
        const clean = pixels(original);
        let total = 0;
        let different = 0;
        for (let i = 0; i < reference.length; i += 4) {
            const delta = (Math.abs(reference[i] - clean[i])
                + Math.abs(reference[i + 1] - clean[i + 1])
                + Math.abs(reference[i + 2] - clean[i + 2])) / 3;
            total += delta;
            if (delta > 24) different++;
        }
        const count = reference.length / 4;
        return total / count <= 9 && different / count <= 0.08;
    }

    function render(target, showEdit) {
        target.drawImage(showEdit ? original : image, 0, 0);
        if (!showEdit) return;

        fields.forEach((field, index) => {
            const value = field.value.trim();
            const box = layout[index];
            if (!box || !value) return;
            const camera = byId('Style').value === 'camera';
            const fontSize = (camera ? Math.max(11, Math.round(image.width * 0.036))
                : fittedFontSize(box)) * Number(byId('Size').value) / 100;
            target.font = camera
                ? '500 ' + fontSize + 'px "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif'
                : fontSize + 'px "Times New Roman", serif';
            target.textAlign = 'right';
            target.textBaseline = 'alphabetic';
            target.fillStyle = '#fff';
            target.shadowColor = 'rgba(0,0,0,.8)';
            const scale = target.getTransform().a;
            target.shadowBlur = scale * (camera ? Math.max(2, fontSize * 0.14) : fontSize * 0.09);
            target.shadowOffsetY = scale * (camera ? Math.max(1, fontSize * 0.05) : fontSize * 0.035);
            const metrics = target.measureText(value);
            const baseline = region.y + (box.y0 + box.y1
                + metrics.actualBoundingBoxAscent - metrics.actualBoundingBoxDescent) / 2
                + image.height * Number(byId('OffsetY').value) / 100;
            const right = Math.max(2, Math.min(image.width, region.x + box.x1
                + image.width * Number(byId('OffsetX').value) / 100));
            target.fillText(value, right, baseline, Math.max(1, right - 2));
        });
    }

    function fittedFontSize(box) {
        const heights = layout.filter(Boolean).map(row => row.y1 - row.y0)
            .filter(height => height > 0).sort((a, b) => a - b);
        const typical = heights[Math.floor((heights.length - 1) / 2)];
        const boxHeight = box.y1 - box.y0;
        const limit = image.width * 0.041;
        return Math.max(1, Math.min(boxHeight * 1.22, typical * 1.22, limit));
    }

    function isolateText(crop, area) {
        const clean = document.createElement('canvas');
        clean.width = crop.width;
        clean.height = crop.height;
        const source = clean.getContext('2d');
        source.drawImage(original, area.x, area.y, area.width, area.height, 0, 0, clean.width, clean.height);
        const base = source.getImageData(0, 0, clean.width, clean.height).data;
        const ink = crop.getContext('2d');
        const patch = ink.getImageData(0, 0, crop.width, crop.height);
        for (let p = 0; p < patch.data.length; p += 4) {
            const delta = Math.min(patch.data[p] - base[p], patch.data[p + 1] - base[p + 1],
                patch.data[p + 2] - base[p + 2]);
            const gray = Math.max(0, 255 - Math.max(0, delta - 12) * 10);
            patch.data[p] = patch.data[p + 1] = patch.data[p + 2] = gray;
            patch.data[p + 3] = 255;
        }
        source.putImageData(patch, 0, 0);
        return clean;
    }

    function preview() {
        sync();
        if (!image) return;
        const scale = Math.min(1, 1200 / Math.max(image.width, image.height));
        canvas.width = Math.round(image.width * scale);
        canvas.height = Math.round(image.height * scale);
        context.scale(scale, scale);
        render(context, edited && pairReady());
        if (!byId('ZoomPane').hidden) {
            zoom.width = region.width;
            zoom.height = region.height;
            const detail = zoom.getContext('2d');
            detail.translate(-region.x, -region.y);
            render(detail, edited && pairReady());
        }
        if (selecting) {
            const box = selection || region;
            context.strokeStyle = '#ff314d';
            context.lineWidth = 2 / scale;
            context.strokeRect(box.x, box.y, box.width, box.height);
        }
    }

    function populate(values) {
        fields.forEach((field, index) => {
            field.value = values[index] || '';
            field.readOnly = !layout[index];
        });
    }

    function cleanLine(text, index) {
        if (index === 0) {
            return text.match(/\b\d{1,2}[/.\-]\d{1,2}[/.\-]\d{2,4}\s+\d{1,2}:\d{2}(?::\d{2})?\b/)?.[0] || '';
        }
        if (index === 1) {
            return text.match(/[NS]\s*\d[^\p{L}]*[EW]\s*\d[^\p{L}]*/iu)?.[0].trim()
                || text.match(/\b\d{1,3}[.,]\d{3,}\s*[,; ]\s*\d{1,3}[.,]\d{3,}\b/)?.[0] || '';
        }
        if (index === 3) {
            return text.match(/[\p{L}\d.-]+(?:\s*_\s*[\p{L}\d.-]+)+/u)?.[0].replace(/\s+/g, '') || '';
        }
        return text;
    }

    function classify(lines) {
        const date = lines.find(line => cleanLine(line.text, 0));
        const gps = lines.find(line => line !== date && cleanLine(line.text, 1));
        const code = [...lines].reverse().find(line => line !== date && line !== gps && cleanLine(line.text, 3));
        const address = lines.find(line => line !== date && line !== gps && line !== code
            && (!gps || line.box.y0 > gps.box.y0) && (!code || line.box.y0 < code.box.y0)
            && (line.text.match(/\p{L}{2,}/gu) || []).length >= 4);
        return [date, gps, address, code];
    }

    async function scan(single = -1) {
        if (!pairReady() || busy) return;
        busy = true;
        edited = false;
        selecting = false;
        selection = null;
        if (single < 0) {
            layout = [];
            recognized = [];
            populate(recognized);
        }
        const area = single < 0 ? region : selectionForScan;
        preview();
        status('Đang nhận diện watermark…');
        let worker;
        try {
            if (!window.Tesseract) throw new Error('OCR unavailable');
            const crop = document.createElement('canvas');
            const scale = Math.min(2, 2200 / Math.max(area.width, area.height));
            crop.width = Math.max(1, Math.round(area.width * scale));
            crop.height = Math.max(1, Math.round(area.height * scale));
            const ocr = crop.getContext('2d', { willReadFrequently: true });
            ocr.drawImage(image, area.x, area.y, area.width, area.height, 0, 0, crop.width, crop.height);
            worker = await Tesseract.createWorker(['vie', 'eng']);
            await worker.setParameters({ tessedit_pageseg_mode: single < 0 ? Tesseract.PSM.SINGLE_BLOCK : Tesseract.PSM.SINGLE_LINE });
            const result = await worker.recognize(pairReady() ? isolateText(crop, area) : crop, {}, { blocks: true, text: true });
            const parseLines = result => (result.data.lines || []).filter(line => line.text.trim()).map(line => ({
                text: line.text.trim(),
                box: Object.fromEntries(Object.entries(line.bbox).map(([name, value]) => [name, Math.round(value / scale)]))
            })).sort((a, b) => a.box.y0 - b.box.y0);
            const lines = parseLines(result);
            if (single >= 0) {
                const text = cleanLine(result.data.text.trim().replace(/\s+/g, ' '), single);
                const box = lines[0]?.box;
                layout[single] = box ? {
                    x0: box.x0 + area.x - region.x, y0: box.y0 + area.y - region.y,
                    x1: box.x1 + area.x - region.x, y1: box.y1 + area.y - region.y
                } : {
                    x0: area.x - region.x, y0: area.y - region.y,
                    x1: area.x + area.width - region.x, y1: area.y + area.height - region.y
                };
                recognized[single] = text;
                fields[single].readOnly = false;
                fields[single].value = text;
                status(text ? 'Đã đọc dòng đã khoanh. Hãy kiểm tra lại.' : 'Chưa đọc được dòng đã khoanh. Bạn có thể nhập thủ công.');
                return;
            }
            const matches = classify(lines);
            if (pairReady() && matches.some(line => !line)) {
                const fallback = classify(parseLines(await worker.recognize(crop, {}, { blocks: true, text: true })));
                matches.forEach((line, index) => {
                    const candidate = fallback[index];
                    if (line || !candidate) return;
                    const center = (candidate.box.y0 + candidate.box.y1) / 2;
                    if (!matches.some(existing => existing && center >= existing.box.y0
                        && center <= existing.box.y1)) matches[index] = candidate;
                });
            }
            recognized = matches.map((line, index) => line ? cleanLine(line.text, index) : '');
            layout = matches.map(line => line?.box);
            populate(recognized);
            const count = matches.filter(Boolean).length;
            status(count
                ? 'Đã nhận diện ' + count + '/4 mục. Hãy kiểm tra lại các ô, nhất là GPS.'
                : 'Chưa tách được các dòng. Hãy khoanh sát vùng chữ và quét lại.');

        } catch (error) {
            status('Không chạy được OCR. Kiểm tra kết nối mạng hoặc nhập thủ công.');
        } finally {
            try {
                if (worker) await worker.terminate();
            } finally {
                busy = false;
                preview();
            }
        }
    }

    async function loadImage(input) {
        const file = input.files[0];
        if (!file) return;
        if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 25 * 1024 * 1024) {
            input.value = '';
            status('Hãy chọn ảnh JPG hoặc PNG tối đa 25 MB.');
            return;
        }
        busy = true;
        sync();
        let next;
        try {
            next = await createImageBitmap(file);
            if (next.width * next.height > 32_000_000) {
                next.close();
                input.value = '';
                status('Ảnh vượt giới hạn 32 megapixel.');
                return;
            }
            if (input === originalInput) {
                original?.close();
                original = next;
            } else {
                image?.close();
                image = next;
                filename = file.name.replace(/\.[^.]+$/, '');
                const x = Math.floor(image.width * 0.10);
                const y = Math.floor(image.height * 0.81);
                region = {x, y, width: image.width - x, height: image.height - y};
                layout = [];
                recognized = [];
                populate(recognized);
                canvas.hidden = false;
            }
        } catch (error) {
            input.value = '';
            status('Không đọc được ảnh đã chọn.');
            return;
        } finally {
            busy = false;
            edited = false;
            preview();
        }
        pairAligned = Boolean(image && original && matchesFrame());
        preview();
        if (image && original && !pairReady()) {
            status('Hai ảnh khác kích thước hoặc nội dung ngoài vùng chữ bị lệch. Hãy chọn lại hai bản của cùng ảnh.');
        } else if (pairReady()) {
            await scan();
        } else {
            status(image ? 'Chọn thêm ảnh gốc sạch để nhận diện.' : 'Chọn thêm ảnh có watermark để nhận diện.');
        }
    }

    fileInput.addEventListener('change', () => loadImage(fileInput));
    originalInput.addEventListener('change', () => loadImage(originalInput));

    function point(event) {
        const box = canvas.getBoundingClientRect();
        return {
            x: Math.round(Math.max(0, Math.min(1, (event.clientX - box.left) / box.width)) * image.width),
            y: Math.round(Math.max(0, Math.min(1, (event.clientY - box.top) / box.height)) * image.height)
        };
    }

    canvas.addEventListener('pointerdown', event => {
        if (!selecting || drag || event.button !== 0) return;
        drag = { start: point(event), id: event.pointerId };
        canvas.setPointerCapture(event.pointerId);
    });
    canvas.addEventListener('pointermove', event => {
        if (!drag || drag.id !== event.pointerId) return;
        const end = point(event);
        const {start} = drag;
        selection = {
            x: Math.min(start.x, end.x),
            y: Math.min(start.y, end.y),
            width: Math.abs(start.x - end.x),
            height: Math.abs(start.y - end.y)
        };
        preview();
    });
    canvas.addEventListener('pointerup', event => {
        if (!drag || drag.id !== event.pointerId) return;
        drag = null;
        if (!selection || selection.width < 12 || selection.height < 12) {
            selection = null;
            preview();
            return;
        }
        const single = Number(byId('ScanMode').value);
        if (single < 0) {
            region = selection;
        } else {
            selectionForScan = selection;
            if (selection.x < region.x || selection.y < region.y
                || selection.x + selection.width > region.x + region.width
                || selection.y + selection.height > region.y + region.height) {
                status('Dòng riêng phải nằm trong vùng quét chung. Hãy khoanh lại toàn bộ watermark trước.');
                preview();
                return;
            }
        }
        scan(single);
    });
    canvas.addEventListener('pointercancel', () => {
        drag = null;
        selection = null;
        preview();
    });

    byId('Select').addEventListener('click', () => {
        edited = false;
        selecting = !selecting;
        selection = null;
        status(selecting ? 'Kéo để khoanh sát quanh khối chữ cũ.' : 'Đã thoát chế độ khoanh vùng.');
        preview();
    });
    byId('Zoom').addEventListener('click', () => {
        const pane = byId('ZoomPane');
        pane.hidden = !pane.hidden;
        byId('Zoom').setAttribute('aria-expanded', String(!pane.hidden));
        preview();
        if (!pane.hidden) pane.scrollLeft = pane.scrollWidth;
    });
    ['Style', 'Size', 'OffsetX', 'OffsetY'].forEach(id => {
        byId(id).addEventListener('input', () => showVersion(true));
    });
    byId('Scan').addEventListener('click', () => scan());
    function showVersion(showEdit) {
        edited = showEdit && pairReady() && fields.every((field, index) => layout[index] && field.value.trim());
        selecting = false;
        selection = null;
        preview();
    }
    byId('Before').addEventListener('click', () => showVersion(false));
    byId('After').addEventListener('click', () => showVersion(true));
    byId('Fields').addEventListener('input', () => showVersion(true));
    byId('Reset').addEventListener('click', () => {
        populate(recognized);
        showVersion(true);
    });
    byId('Download').addEventListener('click', () => {
        if (busy || !pairReady() || !fields.every((field, index) => layout[index] && field.value.trim())) {
            status('Cần đủ hai ảnh cùng kích thước và cả bốn dòng watermark.');
            return;
        }
        showVersion(true);
        busy = true;
        sync();
        const output = document.createElement('canvas');
        output.width = image.width;
        output.height = image.height;
        render(output.getContext('2d'), true);
        output.toBlob(blob => {
            busy = false;
            sync();
            if (!blob) {
                status('Không tạo được ảnh xuất.');
                return;
            }
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = filename + '-watermark.png';
            link.click();
            setTimeout(() => URL.revokeObjectURL(url), 30000);
        }, 'image/png');
    });
})();