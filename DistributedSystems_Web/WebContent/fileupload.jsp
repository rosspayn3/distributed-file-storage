<!--  
 * Names:         Colton Key, Ross Payne, and Julton Sword
 * Assignment:    Final Project - LionDB Distributed Server
 * Class:         CS 3003 - Distributed Systems (4:00 - 5:15 PM)
 */
-->

<%@ page 
language="java"
contentType="text/html; charset=ISO-8859-1"
pageEncoding="ISO-8859-1"
import="java.io.*, java.net.*, edu.uafs.WebClient"
%>
    
<!DOCTYPE html>
<html>
<head>
	<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css" integrity="sha384-Gn5384xqQ1aoWXA+058RXPxPg6fy4IWvTNh0E263XmFcJlSAwiGgFAW/dAiS6JXm" crossorigin="anonymous">
	<script src="https://code.jquery.com/jquery-3.5.1.min.js"></script>
	<title>File Upload</title>
	<meta charset="ISO-8859-1">
	<style>
		body{
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
		if( client == null ){
			client = new WebClient(session, out);
		} else if ( client.getSocket() == null){
			client.connect(out);
		}
		session.setAttribute("client", client);
		
		if(session.getAttribute("username") == null){
			response.sendRedirect("login.jsp");	
		}
			
		
	%>

	<div class="container mt-3">
	
		<h1 class="text-center">Welcome to the <span class="text-info">upload</span> page, <span class="text-info">${username}</span>!</h1>
		<h4 class="text-center text-primary">Time to have some fun!</h4>
	
		<!-- begin form container -->
		<div class="mx-auto mt-5 text-center" style="max-width:500px">
		
			<form action="fileupload" method="POST" enctype="multipart/form-data">
			 
			 	<div class="form-group">
			 		<p>Choose a file:</p>
					<div class="custom-file">
						
						<input class="custom-file-input" type="file" id="fileupload" name="file">
						<label class="custom-file-label" for="fileupload"></label>
					</div>
					<p><small class="text-warning">*Max file size is 200MB</small></p>
					
					<button class="mt-3 btn btn-primary" type="submit">UPLOAD</button>
					
				</div>
				
			</form>
		
		</div>
		<!-- end form container -->
		
		
		<span class="my-3"></span>
		
		<div>
			<p class="mt-2 text-center text-success">${successmsg}</p>
			<p class="mt-2 text-center text-danger">${errormsg}</p>
			<p class="mt-2 text-center text-danger">${errordetails}</p>
		</div>
		
		
		
		
		
		
		
		
	</div>

	<script>
		// Add the following code if you want the name of the file appear on select
		$(".custom-file-input").on("change", function() {
			var fileName = $(this).val().split("\\").pop();
			$(this).siblings(".custom-file-label").addClass("selected").html(fileName);
		});
	</script>

</body>
</html>