$(function () {
    $('.startSimilarityCheck-button').click(function (e) {
        var clickedBtn = $(this);
        var msgBox = clickedBtn.siblings('.all-tests-res');

        var exerciseid = clickedBtn.attr('data-exerciseid');
        var sheetid = clickedBtn.attr('data-sheetid');

        $.ajax({
            type: 'POST',
            url: window.contextPath + "exercise/" + exerciseid + "/sheet/" + sheetid + "/similarityCheckerStartTest",
            beforeSend: function (xhr) {
                xhr.setRequestHeader('X-CSRF-TOKEN', window.csrftoken);
            }
        }).done(function (response) {
            msgBox.text("Es wird geprüft");
        }).fail(function (req, status, err) {
            msgBox.text('Etwas ist schief gelaufen.');
        })

    });
});