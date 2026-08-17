document.addEventListener('DOMContentLoaded', function() {
  document.querySelectorAll('.stat-card .value').forEach(function(el) {
    var target = parseInt(el.textContent, 10);
    if (isNaN(target)) return;
    var current = 0;
    var step = Math.max(1, Math.ceil(target / 30));
    el.textContent = '0';
    var timer = setInterval(function() {
      current += step;
      if (current >= target) {
        el.textContent = target;
        clearInterval(timer);
      } else {
        el.textContent = current;
      }
    }, 25);
  });

  document.querySelectorAll('.card, .stat-card').forEach(function(card, i) {
    card.style.opacity = '0';
    card.style.transform = 'translateY(16px)';
    setTimeout(function() {
      card.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
      card.style.opacity = '1';
      card.style.transform = 'translateY(0)';
    }, 60 * i);
  });
});
