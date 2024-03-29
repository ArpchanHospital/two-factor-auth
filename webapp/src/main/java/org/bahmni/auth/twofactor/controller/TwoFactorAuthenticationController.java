package org.bahmni.auth.twofactor.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bahmni.auth.smsinterface.SmsGateWay;
import org.bahmni.auth.twofactor.ResponseConstants;
import org.bahmni.auth.twofactor.database.Database;
import org.bahmni.auth.twofactor.model.Contact;
import org.bahmni.auth.twofactor.model.OTP;
import org.bahmni.auth.twofactor.service.OTPService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@EnableAutoConfiguration
@RestController
public class TwoFactorAuthenticationController {
    private static Logger logger = LogManager.getLogger(TwoFactorAuthenticationController.class);

    @Autowired
    private OTPService otpService;
    @Autowired
    private SmsGateWay smsGateWay;
    @Autowired
    private Database database;

    @RequestMapping(path = "/send", method = RequestMethod.GET)
    public boolean sendOTP(@RequestParam(name = "userName") String userName) {
        OTP otp = otpService.generateAndSaveOtpFor(userName);
        return sendSMS(userName, otp.toString());
    }

    @RequestMapping(path = "/resend", method = RequestMethod.GET)
    public String resendOTP(@RequestParam(name = "userName") String userName) {
        OTP otp = otpService.regenerateAndSaveOtpFor(userName);
        if (otp == null) {
            return ResponseConstants.MAX_RESEND_ATTEMPTS_EXCEEDED;
        }
        return String.valueOf(sendSMS(userName, otp.toString()));
    }

    private boolean sendSMS(String userName, String otp) {
        Contact contact = database.findMobileNumberByUserName(userName);
        if (contact != null) {
            smsGateWay.sendSMS(contact.getCountryCode(), contact.getMobileNumber(), otp);
            logger.info("SMS sent to {} carrying OTP {}", userName, otp);
            return true;
        }
        return false;
    }

    @RequestMapping(path = "/validate", method = RequestMethod.GET)
    public String validateOTP(@RequestParam(name = "userName") String userName, @RequestParam(name = "otp") String receivedOTP) {
        return otpService.validateOTPFor(userName, receivedOTP);
    }

}
