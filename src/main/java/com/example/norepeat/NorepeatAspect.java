package com.example.norepeat;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;

@Component
@Aspect
@ConditionalOnClass(RedisTemplate.class)
public class NorepeatAspect {
    private static final String KEY_TEMPLATE = "norepeat_%s";
 
	@Resource
	private RedisTemplate<String, String> redisTemplate;
	@Resource
	private RedisService redisService;

	@Pointcut("@annotation(com.example.norepeat.Norepeat)")
	public void executeNorepeat() {
 
	}
 
	@Around("executeNorepeat()")
	public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
		Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
		Norepeat norepeat = method.getAnnotation(Norepeat.class);
		String generateKey = KeyUtil.generate(method, joinPoint.getArgs());
		String key = String.format(KEY_TEMPLATE,norepeat.value() + "_" +generateKey);
		boolean setKey = redisService.setNx(key, key, norepeat.expireMillis());
		if (setKey) {
			return joinPoint.proceed();
		} else {
			throw new RepeatException("已经重复提交了, key=" + key);
		}
	}
}