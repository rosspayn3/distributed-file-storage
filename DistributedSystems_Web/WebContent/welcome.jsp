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
	<meta charset="ISO-8859-1">
	<title>Welcome <% out.print(session.getAttribute("username")); %>!</title>
	<style>
		body{
			background-color: #333;
			color: #EEE;
		}
		
	</style>
</head>
<body>

	<%
	
		// creates a new WebClient if this is the first time a user has opened a page,
		// then saves it as a session variable.
		// WebClient houses the functions to be called in the servlets.
		if(session.getAttribute("client") == null){
			WebClient client = new WebClient(session, out);
			session.setAttribute("client", client);
		}
		
	%>

	<div class="container">
	
		<h1 class="text-center">Welcome, <span class="text-info">${username}</span>!</h1>
		<h4 class="text-center text-primary">Time to have some fun!</h4>
	
		<div class="mt-5">
		
			<p>Username: <strong>${username}</strong></p>
			<p>Password: <strong>${password}</strong></p>
			<p>
			Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. 
			Orci sagittis eu volutpat odio facilisis mauris sit amet massa. Neque ornare aenean euismod elementum nisi quis eleifend quam. 
			Pellentesque habitant morbi tristique senectus et. Ullamcorper morbi tincidunt ornare massa eget egestas purus viverra. 
			Adipiscing elit ut aliquam purus sit amet luctus venenatis. Ultrices mi tempus imperdiet nulla. Viverra aliquet eget sit amet tellus cras. 
			Arcu cursus euismod quis viverra nibh cras pulvinar mattis nunc. Dictumst vestibulum rhoncus est pellentesque elit ullamcorper 
			dignissim cras tincidunt. Consectetur adipiscing elit pellentesque habitant morbi tristique. Ac feugiat sed lectus vestibulum 
			mattis ullamcorper velit sed ullamcorper. Faucibus a pellentesque sit amet porttitor eget dolor morbi.
			</p>
		
		</div>
	
	</div>

</body>
</html>