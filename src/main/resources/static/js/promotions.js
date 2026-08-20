document.addEventListener('DOMContentLoaded', function () {
  var search = document.getElementById('promotionSearch');
  var statusFilter = document.getElementById('statusFilter');
  var yearFilter = document.getElementById('yearFilter');
  var rows = document.querySelectorAll('#promotionRows tr[data-search]');

  function applyFilters() {
    var term = (search ? search.value.trim().toLowerCase() : '');
    var status = statusFilter ? statusFilter.value : '';
    var year = yearFilter ? yearFilter.value : '';
    rows.forEach(function (row) {
      var text = (row.dataset.search || '').toLowerCase();
      var statusOk = !status || row.dataset.status === status;
      var yearOk = !year || row.dataset.year === year;
      var visible = text.indexOf(term) !== -1 && statusOk && yearOk;
      row.style.display = visible ? '' : 'none';
      var editRow = row.nextElementSibling;
      if (editRow && editRow.classList.contains('edit-row')) {
        editRow.style.display = visible && editRow.dataset.open === 'true' ? '' : 'none';
      }
    });
  }

  if (search) {
    search.addEventListener('input', applyFilters);
  }
  if (statusFilter) {
    statusFilter.addEventListener('change', applyFilters);
  }
  if (yearFilter) {
    yearFilter.addEventListener('change', applyFilters);
  }

  document.querySelectorAll('[data-edit-target]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var editRow = document.getElementById('edit-' + btn.dataset.editTarget);
      if (!editRow) return;
      var open = editRow.dataset.open === 'true';
      editRow.dataset.open = open ? 'false' : 'true';
      editRow.hidden = open;
    });
  });
  document.querySelectorAll('.edit-cancel').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var editRow = btn.closest('tr.edit-row');
      if (editRow) {
        editRow.dataset.open = 'false';
        editRow.hidden = true;
      }
    });
  });

  var entryYear = document.getElementById('createEntryYear');
  var graduationYear = document.getElementById('createGraduationYear');
  if (entryYear && graduationYear) {
    entryYear.addEventListener('input', function () {
      var year = parseInt(entryYear.value, 10);
      if (!isNaN(year)) {
        graduationYear.value = year + 3;
      }
    });
  }

  var trackSelect = document.getElementById('trackFilter');
  if (trackSelect) {
    var trackSubmit = document.getElementById('trackSubmit');
    function syncTrack() {
      if (trackSubmit) trackSubmit.value = trackSelect.value;
    }
    syncTrack();
    trackSelect.addEventListener('change', function () {
      syncTrack();
      var params = new URLSearchParams(window.location.search);
      if (trackSelect.value) {
        params.set('track', trackSelect.value);
      } else {
        params.delete('track');
      }
      var query = params.toString();
      window.location.href = window.location.pathname + (query ? '?' + query : '');
    });
  }

  var groupFilter = document.getElementById('groupFilter');
  if (groupFilter) {
    groupFilter.addEventListener('change', function () {
      var params = new URLSearchParams(window.location.search);
      if (groupFilter.value) {
        params.set('group', groupFilter.value);
      } else {
        params.delete('group');
      }
      var query = params.toString();
      window.location.href = window.location.pathname + (query ? '?' + query : '');
    });
  }

  document.querySelectorAll('.stat-value[data-count]').forEach(function (el) {
    var target = parseInt(el.textContent, 10);
    if (isNaN(target) || el.textContent.indexOf('.') !== -1) return;
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