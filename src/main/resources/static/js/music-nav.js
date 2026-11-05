(() => {
    'use strict';
    const link = document.getElementById('custMapMusicLink');
    if (!link) return;

    link.addEventListener('click', event => {
        if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        const player = window.open('', link.target);
        if (!player) return;
        event.preventDefault();
        try {
            if (!player.document.getElementById('music')) player.location.replace(link.href);
        } catch (error) {
            player.location = link.href;
        }
        player.focus();
    });
})();
