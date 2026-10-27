// Loads the sections of the Specs page one by one, so the page appears at once
// instead of waiting for the node to answer.
(function () {
  document.querySelectorAll("[data-specs-section]").forEach(function (target) {
    fetch(target.dataset.specsSection, { credentials: "same-origin" })
      .then(function (response) {
        if (!response.ok) {
          throw new Error(response.status);
        }
        return response.text();
      })
      .then(function (html) {
        target.innerHTML = html;
      })
      .catch(function () {
        target.textContent = "This section could not be loaded. Reload the page to try again.";
      });
  });
})();
