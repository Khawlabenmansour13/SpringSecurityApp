package tn.spring.security.controllers;


import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import tn.spring.payload.request.LoginRequest;
import tn.spring.payload.response.JwtResponse;
import tn.spring.payload.response.MessageResponse;
import tn.spring.security.entities.User;
import tn.spring.security.services.EmailService;
import tn.spring.security.services.IRoleService;
import tn.spring.security.services.IUserService;
import tn.spring.security.services.UserService;
import tn.spring.security.ssecurity.CustomLoginSuccessHandler;
import tn.spring.security.ssecurity.Jwt.JwtUtils;
import tn.spring.security.ssecurity.services.UserDetailsImpl;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/User/Access")
public class AuthController {
	@Autowired
	AuthenticationManager authenticationManager;
	@Autowired
	CustomLoginSuccessHandler customLoginSuccessHandler;//authentification success
	@Autowired
	IUserService iuserservice;

	@Autowired
	IRoleService iroleservice;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	JwtUtils jwtUtils;

	@Autowired
	private EmailService emailService;

	@PostMapping("/login")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) throws Exception {
		boolean verifMdp = customLoginSuccessHandler.onAuthenticationSuccess(loginRequest.getUsername(),
				loginRequest.getPassword());
		
		System.out.println("username = "+loginRequest.getUsername());
		if (verifMdp) {
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
			SecurityContextHolder.getContext().setAuthentication(authentication);
			String jwt = jwtUtils.generateJwtToken(authentication);

			UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
			List<String> roles = userDetails.getAuthorities().stream().map(item -> item.getAuthority())
					.collect(Collectors.toList());

			return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getUsername(), roles.get(0).toString()));
		}
		return ResponseEntity.ok("Une erreur est produit verifier vos coordonées");
	}

	@PostMapping("/signup")
	@ResponseBody
	public ResponseEntity<?> createUser(@RequestBody User user) throws Exception {
		if (user == null) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: please add values!"));
		}
		//if (user.getAddress().equals("")) {
			//return ResponseEntity.badRequest().body(new MessageResponse("Error: please add address!"));
		//}
	//	if (!(user.getBirthdate() instanceof Date)) {
		//	return ResponseEntity.badRequest().body(new MessageResponse("Error: please add bithday date!"));
		//}
		if (user.getFirstName().equals("")) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: please add your first name!"));
		}
		if (user.getMail().equals("") || !UserService.validate(user.getMail())) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: please check your mail!"));
		}
		if (iuserservice.findUserBylogin(user.getLogin()) != null) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
		}
		// Create new user's account
		user.setPassword(encoder.encode(user.getPassword()));
		if (user.getRole() == null) {
			return ResponseEntity.badRequest()
					.body(new MessageResponse("Error: Could you please add a role for the new user!"));
		}
		user.setValid(true);
		user.setAccountNonLocked(true);
		user.setFailedAttempt(0);
		//SEND MAIL
		SimpleMailMessage passwordResetEmail = new SimpleMailMessage();
		passwordResetEmail.setTo(user.getMail());
		passwordResetEmail.setSubject("Registration");
		passwordResetEmail.setText(
				"Dear " + user.getFirstName() + ":\n" + "Félicitation !" 
						+ ":\n" 
						+ "Your role is " + user.getRole().getRoleType().name()
						+ ".\n"
						+"Team MyDoctor") ;

		emailService.sendEmail(passwordResetEmail);
		iuserservice.createUser(user);
		return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
	}

	@PostMapping("/forgot/{login}")
	public String processForgotPasswordForm(@PathVariable("login") String login,
			HttpServletRequest request) throws Exception {
		User user = iuserservice.findUserBylogin(login);

		if (user == null) {
			return "user not found";
		} else {
			// Generate random 36-character string token for reset password
			user.setResettoken(UUID.randomUUID().toString());

			// Save token to database
			iuserservice.updateUser(user);

			String appUrl = request.getServerName()+":"+request.getServerPort()+request.getContextPath();

			// Email message
			SimpleMailMessage passwordResetEmail = new SimpleMailMessage();
			passwordResetEmail.setTo(user.getMail());
			passwordResetEmail.setSubject("Password Reset Request");
			passwordResetEmail.setText("To reset your password, click the link below:\n" + appUrl + "/servlet/User/Access/reset/"
					+ user.getResettoken());

			emailService.sendEmail(passwordResetEmail);
		}
		return "mail sent";

	}

	// Process reset password form
	@PostMapping("/reset/{token}/{newpassword}")
	public String setNewPassword(@PathVariable("token") String token,@PathVariable("newpassword") String newpassword ) throws Exception {
		User user = iuserservice.findUserByResetToken(token);
		if (user != null) {
			user.setPassword(encoder.encode(newpassword));
			user.setResettoken(null);
			iuserservice.updateUser(user);
			return "passwored reseted";

		} else {
			return "operation regected";
		}
	}
}