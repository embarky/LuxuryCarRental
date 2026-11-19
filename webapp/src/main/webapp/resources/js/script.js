// Hide page messages after a few seconds; error messages stay until clicked.
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.toasts li').forEach(item => {
        item.style.cursor = 'pointer';
        item.title = 'Click to close';
        item.addEventListener('click', () => item.remove());
        if (!item.classList.contains('toast-error')) {
            setTimeout(() => item.remove(), 7000);
        }
    });
});
