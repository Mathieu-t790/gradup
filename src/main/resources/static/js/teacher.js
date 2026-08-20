document.addEventListener('DOMContentLoaded', function () {
  var semesterFilter = document.getElementById('semesterFilter');
  var courseRows = document.querySelectorAll('tbody tr[data-semester]');
  if (semesterFilter && courseRows.length) {
    semesterFilter.addEventListener('change', function () {
      courseRows.forEach(function (row) {
        var visible =
          !semesterFilter.value || row.dataset.semester === semesterFilter.value;
        row.style.display = visible ? '' : 'none';
      });
    });
  }

  var studentSearch = document.getElementById('studentSearch');
  var studentRows = document.querySelectorAll('tbody tr[data-search]');
  if (studentSearch) {
    studentSearch.addEventListener('input', function () {
      var term = studentSearch.value.trim().toLowerCase();
      studentRows.forEach(function (row) {
        var text = (row.dataset.search || '').toLowerCase();
        var visible = text.indexOf(term) !== -1;
        row.style.display = visible ? '' : 'none';
      });
    });
  }
});