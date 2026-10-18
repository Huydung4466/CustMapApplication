package com.huydung.custmapapplication.kmz.service.impl;

import com.huydung.custmapapplication.kmz.converter.KmzConverter;
import com.huydung.custmapapplication.kmz.dto.KmzImportResultDTO;
import com.huydung.custmapapplication.kmz.dto.KmzLocationDTO;
import com.huydung.custmapapplication.kmz.repository.KmzLocationRepository;
import com.huydung.custmapapplication.kmz.service.KmzService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class KmzServiceImpl implements KmzService {

    private final KmzLocationRepository kmzLocationRepository;
    private final KmzConverter kmzConverter;

    public KmzServiceImpl(KmzLocationRepository kmzLocationRepository, KmzConverter kmzConverter) {
        this.kmzLocationRepository = kmzLocationRepository;
        this.kmzConverter = kmzConverter;
    }

    @Override
    @Transactional
    public KmzImportResultDTO importKmzFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File KMZ không được để trống!");
        }

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown.kmz";
        List<KmzLocationDTO> parsedLocations = new ArrayList<>();

        try (InputStream is = file.getInputStream()) {
            if (fileName.toLowerCase().endsWith(".kml")) {
                // Hỗ trợ cả trường hợp người dùng upload thẳng file .kml đã giải nén
                parsedLocations.addAll(parseKmlStream(is, fileName));
            } else {
                // Giải nén file .kmz để tìm file .kml bên trong (VD: doc.kml)
                try (ZipInputStream zis = new ZipInputStream(is)) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".kml")) {
                            byte[] kmlBytes = readCurrentZipEntry(zis);
                            parsedLocations.addAll(parseKmlStream(new ByteArrayInputStream(kmlBytes), fileName));
                        }
                        zis.closeEntry();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi đọc file KMZ: " + e.getMessage(), e);
        }

        // Khử trùng lặp ngay trong cùng 1 file KMZ (nếu có 2 Placemark trùng mã tủ trong cùng file)
        Map<String, KmzLocationDTO> uniqueMap = new LinkedHashMap<>();
        for (KmzLocationDTO loc : parsedLocations) {
            uniqueMap.put(loc.getCabinetCode().trim().toUpperCase(), loc);
        }

        List<KmzLocationDTO> uniqueList = new ArrayList<>(uniqueMap.values());
        int savedCount = kmzLocationRepository.batchUpsertLocations(uniqueList);

        String msg = String.format("Import thành công file %s: Đọc được %d điểm, đã lưu/cập nhật %d mã tủ.",
                fileName, parsedLocations.size(), savedCount);

        return new KmzImportResultDTO(fileName, parsedLocations.size(), savedCount, msg);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<KmzLocationDTO> getLocationByCabinetCode(String cabinetCode) {
        if (cabinetCode == null || cabinetCode.trim().isEmpty()) {
            return Optional.empty();
        }
        return kmzLocationRepository.findByCabinetCodeIgnoreCase(cabinetCode.trim())
                .map(kmzConverter::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotalLocations() {
        return kmzLocationRepository.count();
    }

    // Đọc dữ liệu của 1 file KML bên trong gói ZIP ra mảng byte an toàn
    private byte[] readCurrentZipEntry(ZipInputStream zis) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;
        while ((len = zis.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }

    // Phân tích cú pháp XML KML bằng StAX (Tối ưu cho file dữ liệu lớn)
    private List<KmzLocationDTO> parseKmlStream(InputStream kmlInputStream, String sourceFileName) throws Exception {
        List<KmzLocationDTO> results = new ArrayList<>();

        XMLInputFactory factory = XMLInputFactory.newInstance();
        // Chống lỗi bảo mật XXE và bỏ qua DTD bên ngoài
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        XMLStreamReader reader = factory.createXMLStreamReader(kmlInputStream, "UTF-8");

        boolean inPlacemark = false;
        String currentTag = null;
        String placemarkName = null;
        String coordinatesText = null;

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();
                if ("Placemark".equalsIgnoreCase(localName)) {
                    inPlacemark = true;
                    placemarkName = null;
                    coordinatesText = null;
                } else if (inPlacemark) {
                    currentTag = localName;
                }
            } else if (event == XMLStreamConstants.CHARACTERS && inPlacemark && currentTag != null) {
                String text = reader.getText().trim();
                if (!text.isEmpty()) {
                    if ("name".equalsIgnoreCase(currentTag)) {
                        placemarkName = (placemarkName == null) ? text : placemarkName + text;
                    } else if ("coordinates".equalsIgnoreCase(currentTag)) {
                        coordinatesText = (coordinatesText == null) ? text : coordinatesText + " " + text;
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                String localName = reader.getLocalName();
                if ("Placemark".equalsIgnoreCase(localName)) {
                    inPlacemark = false;
                    KmzLocationDTO dto = kmzConverter.fromKmlPlacemark(placemarkName, coordinatesText, sourceFileName);
                    if (dto != null) {
                        results.add(dto);
                    }
                }
                currentTag = null;
            }
        }
        reader.close();
        return results;
    }
}
