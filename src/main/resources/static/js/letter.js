$(function(){
	$("#sendBtn").click(send_letter);
	$(".close").click(delete_msg);
});

function send_letter() {
	$("#sendModal").modal("hide");

	var toName = $("#recipient-name").val();
	var content = $("#message-text").val();

	$.post(
	    CONTEXT_PATH + "/letter/send",
	    {
	        "toName": toName,
	        "content": content
	    },
	    function(data) {
	        data = $.parseJSON(data);
	        if(data.code == 0) {
	            $("#hintBody").text("发送成功");
	        } else{
	            $("#hintBody").text(data.message);
	        }

	        $("#hintModal").modal("show");
            setTimeout(function(){
                $("#hintModal").modal("hide");
                location.reload();
            }, 2000);
	    }
	);
}

function delete_msg() {
    var id = $(this).data("id");
    $.ajax({
        url: CONTEXT_PATH + "/letter/delete/" + id,
        type: 'DELETE',
        success: function(result) {
            $(this).parents(".media").remove();
        }.bind(this),
        error: function(xhr, status, error) {
            console.error('Error deleting message:', error);
        }
    });
}