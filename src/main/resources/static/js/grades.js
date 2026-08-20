document.addEventListener('DOMContentLoaded', function () {
  var semesterFilter = document.getElementById('semesterFilter');
  if (semesterFilter) {
    semesterFilter.addEventListener('change', function () {
      var params = new URLSearchParams(window.location.search);
      if (semesterFilter.value) {
        params.set('semesterId', semesterFilter.value);
      } else {
        params.delete('semesterId');
      }
      var query = params.toString();
      window.location.href = window.location.pathname + (query ? '?' + query : '');
    });
  }
});