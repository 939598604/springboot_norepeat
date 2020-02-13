package com.example.norepeat;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
 
@RestController
public class RepeatController {

	@PostMapping("/repeat")
	@Norepeat(value = "/repeat", expireMillis = 5000L)
	public String repeat(@RequestBody User user) {
		return "redis access ok:" + user.getUserName() + " " + user.getUserAge();
	}
}
