$(function(){
	$("#publishBtn").click(publish);
});

function publish() {
	$("#publishModal").modal("hide");
	// 获取标题和内容
	var title = $('#recipient-name').val();
	var content = $('#message-text').val();
	// 发送异步请求
	$.post(
	    CONTEXT_PATH + "/discuss/add",
	    {
	        "title": title,
	        "content": content
	    },
	    function(data) {
	        data = $.parseJSON(data);
	        // 在提示框当中显示返回的消息
            $('#hintBody').text(data.message);
            // 显示提示框
            $("#hintModal").modal("show");
            // 两秒后自动隐藏提示框
            setTimeout(function(){
                $("#hintModal").modal("hide");
                // 刷新页面
                if(data.code==0){
                    window.location.reload();
                }
            }, 2000);
	    }
	)

}