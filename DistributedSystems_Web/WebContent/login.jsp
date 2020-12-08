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
	<title>Login</title>
	<meta charset="ISO-8859-1">
	<style>
	body {
		background-color: #333;
		color: #EEE;
	}
	</style>
</head>
<body>

	


	<nav class="navbar sticky-top navbar-expand-lg navbar-light bg-light">
		<a class="navbar-brand" href="#">Group 10</a>
		<button class="navbar-toggler" type="button" data-toggle="collapse"
			data-target="#navbarNav" aria-controls="navbarNav"
			aria-expanded="false" aria-label="Toggle navigation">
			<span class="navbar-toggler-icon"></span>
		</button>
		<div class="collapse navbar-collapse" id="navbarNav">
			<ul class="navbar-nav">
				<li class="nav-item active"><a class="nav-link text-info font-weight-bold" href="#">Login<span class="sr-only">(current)</span></a></li>
				<li class="nav-item"><a class="nav-link" href="register.jsp">Register</a></li>
				<li class="nav-item"><a class="nav-link" href="fileupload.jsp">Upload</a></li>
				<li class="nav-item"><a class="nav-link" href="files.jsp">Files</a></li>
			</ul>
		</div>
	</nav>

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
		
	%>

	<div class="container mt-3">

		<h1 class="text-center">
			Welcome to the <span class="text-info">login</span> page!
		</h1>
		<h4 class="text-center text-primary">Time to have some fun!</h4>

		<!-- begin form container -->
		<div class="mx-auto mt-5 text-center" style="width: 300px">

			<form action="login" method="POST">

				<div class="form-group">

					<input class="form-control my-2" type="text" id="form-username"
						name="username" placeholder="Username" value="${username}" /> 
					<input class="form-control my-2" type="password" id="form-password"
						name="password" placeholder="Password" />

					<button class="mt-3 btn btn-primary" type="submit">LOGIN</button>

					<p class="mt-3 text-center text-small">
						Need an account? <a href="register.jsp">Register now</a>
					<p class="mt-3 text-center text-danger">${errormsg}</p>
				</div>

			</form>

		</div>
		<!-- end form container -->








	</div>

	<script src="https://code.jquery.com/jquery-3.3.1.slim.min.js"
		integrity="sha384-q8i/X+965DzO0rT7abK41JStQIAqVgRVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo"
		crossorigin="anonymous"></script>
	<script
		src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js"
		integrity="sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1"
		crossorigin="anonymous"></script>
	<script
		src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js"
		integrity="sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM"
		crossorigin="anonymous"></script>


</body>
</html>