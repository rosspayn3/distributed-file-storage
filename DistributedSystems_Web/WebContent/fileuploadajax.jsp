<!--  
 * Names:         Colton Key, Ross Payne, and Julton Sword
 * Assignment:    Final Project - LionDB Distributed Server
 * Class:         CS 3003 - Distributed Systems (4:00 - 5:15 PM)
 */
-->

<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"
	import="java.io.*, java.net.*, edu.uafs.WebClient"%>

<!DOCTYPE html>
<html>
<head>
	<link rel="stylesheet"
		href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css"
		integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T"
		crossorigin="anonymous">
	<script src="https://code.jquery.com/jquery-3.5.1.min.js"></script>
	<title>File Upload</title>
	<meta charset="ISO-8859-1">
	<style>
	body {
		background-color: #333;
		color: #EEE;
	}
	</style>
</head>
<body>

	<%
		// creates a new WebClient if this is the first time a user has opened a page.
		// WebClient houses the functions to be called in the servlets.
		WebClient client = (WebClient) session.getAttribute("client");
		if (client == null) {
			client = new WebClient(session, out);
		} else if (client.getSocket() == null) {
			client.connect(out);
		}
		session.setAttribute("client", client);
		
		String loggedin = (String) session.getAttribute("loggedin");
		
		if (session.getAttribute("username") == null || !loggedin.equalsIgnoreCase("true")) {
			response.sendRedirect("login.jsp");
		}
	%>
	
	
	<nav class="navbar sticky-top navbar-expand-lg navbar-light bg-light">
		<a class="navbar-brand" href="#">Group 10</a>
		<button class="navbar-toggler" type="button" data-toggle="collapse"
			data-target="#navbarNav" aria-controls="navbarNav"
			aria-expanded="false" aria-label="Toggle navigation">
			<span class="navbar-toggler-icon"></span>
		</button>
		<div class="collapse navbar-collapse" id="navbarNav">
			<ul class="navbar-nav">
				<li class="nav-item"><a class="nav-link" href="login.jsp">Login</a></li>
				<li class="nav-item"><a class="nav-link" href="register.jsp">Register</a></li>
				<li class="nav-item active"><a class="nav-link text-info font-weight-bold" href="#">Upload<span class="sr-only">(current)</span></a></li>
			</ul>
		</div>
	</nav>


	<div class="container mt-3">

		<h1 class="text-center">
			Welcome to the <span class="text-info">upload</span> page, <span
				class="text-info">${username}</span>!
		</h1>
		<h4 class="text-center text-primary">Time to have some fun!</h4>

		<!-- begin form container -->
		<div class="mx-auto mt-5 text-center" style="max-width: 500px">

			<form id="fileUploadForm" method="POST" enctype="multipart/form-data">

				<div class="form-group">
					<p>Choose a file:</p>
					
					<div class="custom-file">
						<input class="custom-file-input" type="file" id="fileupload" name="file">
						<label class="custom-file-label" for="fileupload"></label>
					</div>
					<p>
						<small class="text-warning">*Max file size is 200MB</small>
					</p>

					<button class="mt-3 btn btn-primary" id="btnSubmit" type="submit">UPLOAD</button>

				</div>

			</form>

		</div>
		<!-- end form container -->
		
		
		<div class="progress" style="display: none">
			<div class="progress-bar progress-bar-striped progress-bar-animated" style="width: 0%"></div>
		</div>


		<div class="mt-5"id="messages">
			<p class="mt-2 text-center text-success">${successmsg}</p>
			<p class="mt-2 text-center text-danger">${errormsg}</p>
			<p class="mt-2 text-center text-danger">${errordetails}</p>
		</div>








	</div>
	
	<!-- jquery submit -->
	<script type="text/javascript">
	
		$("#btnSubmit").click( function() {
			var formData = new FormData();
			var file = $('input[type=file]')[0].files[0];
			formData.append('file',file);
			$('.progress').show();
			$.ajax({
				xhr: function() {
					var xhr = new window.XMLHttpRequest();
					xhr.upload.addEventListener("progress", function(e) {
						if (e.lengthComputable) {
							var percentComplete = parseInt( (e.loaded / e.total) * 100 );
							$('.progress-bar').css('width',percentComplete+"%");
							if (percentComplete === 100) {
								
							}
						}
					}, false);
					return xhr;
				},
				url: "fileupload",
				type: "POST",
				data: formData,
				contentType: false,
				processData: false,
				success: function(result) {
					console.log(result);
				},
				error: function(result) {
					console.log(result);
				}
			});
		});
		
	</script>
	

	<script "></script>


	<script>
		$(".custom-file-input").on("change", function() {
			var fileName = $(this).val().split("\\").pop();
			$(this).siblings(".custom-file-label").addClass("selected").html(fileName);
		});
	</script>

	
	
	<script
		src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js"
		integrity="sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1"
		crossorigin="anonymous">
	</script>
	
	<script
		src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js"
		integrity="sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM"
		crossorigin="anonymous">
	</script>


</body>
</html>