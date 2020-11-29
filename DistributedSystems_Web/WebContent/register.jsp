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
	
		// creates a new WebClient if this is the first time a user has opened a page.
		// WebClient houses the functions to be called in the servlets.
		if(session.getAttribute("client") == null){
			WebClient client = new WebClient(session, out);
			session.setAttribute("client", client);
		}
		
	%>

	<div class="container">
	
		<h1 class="text-center">Welcome to the <span class="text-info">registration</span> page!</h1>
		<h4 class="text-center text-primary">Time to have some fun!</h4>
	
		<!-- begin form container -->
		<div class="mx-auto mt-5" style="width:350px">
		
			<form action="register" method="POST">
			
				<div class="form-group">
				    
				    <!-- <label class="mt-2" for="form-username">Username:</label> -->
					<input class="form-control my-2" type="text" id="form-username" name="username" placeholder="Username" value="${username}"/>
					
					<!-- <label class="mt-2"for="form-password">Password:</label> -->
					<input class="form-control mt-4 mb-2" type="password" id="form-password" name="password" placeholder="Password" />
					
					<!-- <label class="mt-2"for="form-confirm">Confirm Password:</label> -->
					<input class="form-control my-2" type="password" id="form-confirm" name="confirmpassword" placeholder="Confirm Password"/>
					
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