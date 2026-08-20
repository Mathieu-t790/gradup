document.addEventListener('DOMContentLoaded', function () {
  var search = document.getElementById('promotionSearch');
  var rows = document.querySelectorAll('#promotionRows tr');
  if (search) {
    search.addEventListener('input', function () {
      var term = search.value.trim().toLowerCase();
      rows.forEach(function (row) {
        var text = (row.dataset.search || '').toLowerCase();
        row.style.display = text.indexOf(term) !== -1 ? '' : 'none';
      });
    });
  }

  document.querySelectorAll('.stat-value[data-count]').forEach(function (el) {
    var target = parseInt(el.textContent, 10);
    if (isNaN(target)) return;
    var current = 0;
    var step = Math.max(1, Math.ceil(target / 30));
    el.textContent = '0';
    var timer = setInterval(function () {
      current += step;
      if (current >= target) {
        el.textContent = target;
        clearInterval(timer);
      } else {
        el.textContent = current;
      }
    }, 25);
  });
});