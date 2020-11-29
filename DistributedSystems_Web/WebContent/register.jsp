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
	<title>Register</title>
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
		if(session.getAttribute("client") == null){
			WebClient client = new WebClient(session, out);
			session.setAttribute("client", client);
		}
		
	%>

	<div class="container">
	
		<h1 class="text-center">Welcome to the registration page!</h1>
		<h4 class="text-center text-muted">Time to have some fun!</h4>
	
		<!-- begin form container -->
		<div class="mx-auto mt-5" style="width:300px">
		
			<form action="register" method="POST">
			
				<div class="form-group">
				    
				    <label class="mt-2" for="form-username">Username:</label>
					<input class="form-control" type="text" id="form-username" name="username" value="${username}"/>
					
					<label class="mt-2"for="form-password">Password:</label>
					<input class="form-control" type="password" id="form-password" name="password" />
					
					<label class="mt-2"for="form-confirm">Confirm Password:</label>
					<input class="form-control" type="password" id="form-confirm" name="confirmpassword" />
					
					<div class="text-center">
						<button class="mt-3 btn btn-primary" type="submit">REGISTER</button>
						
						<p class="mt-3 text-center text-small">Already registered? <a href="login.jsp">Sign In</a>
					    <p class="mt-3 text-center text-danger">${errormsg}</p>
				    </div>
				</div>
				
			</form>
		
		</div>
		<!-- end form container -->
		
		
		
		
		
		
		
		
	</div>

</body>
</html>