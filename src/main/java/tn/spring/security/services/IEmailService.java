package tn.spring.security.services;

import org.springframework.mail.SimpleMailMessage;
public interface IEmailService {
	public void sendEmail(SimpleMailMessage email);
}