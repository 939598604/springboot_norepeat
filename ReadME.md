# 如何限制请求5秒内只执行一次

## 一、为什么要禁止重复提交？

禁止重复提交

在我们平时开发的过程中，有很多用户点击提交按钮提交表单或者说用户主动提交某些信息的情景。正常情况下，我们后台正常接收前台提交的内容，然后再进行增删改查等操作。但是，我们都说不能已常理去考虑用户的使用情况。一旦前台提交内容后，因为网络波动或者后台逻辑处理较慢，而前台又没有做禁止点击提交按钮或者等待页面，难免出现用户疯狂点击提交按钮的情况。这种情况就很有可能导致用户的数据多次提交、入库，产生脏数据、冗余数据等情况。

上述只是可能会出现重复提交或者请求的一种情况，实际上，会造成这种情况的场景不少：

网络波动：
因为网络波动，造成重复请求
用户的重复性操作：
用户误操作，或者因为接口响应慢，而导致用户耐性消失，有意多次触发请求
重试机制：
这种情况，经常出现在调用三方接口的时候。对可能出现的异常情况抛弃，然后进行固定次数的接口重复调用，直到接口返回正常结果。
总而言之，禁止重复提交使我们保证数据准确性及安全性的必要操作。

## 二、springBoot+redis禁止重复提交

### 2.1实现思路

首先明确一下思路：

（1）判断统请求是否来自于同一客户端，可以用请求地址中token参数来标识实现。

（2）同一客户端5s内请求同样的url，即视为重复提交。

（3）客户端第一请求控制器，因控制器添加了Norepeat注解，被NorepeatAspect拦截器拦截，进入处理逻辑：

- 通过keyUtil工具类根据token+url 生产MD5的作为redis的key
- 利用redis中的setNx特性，设置key值，同时设置过期时间为5S,并执行控制器的业务逻辑
- 如果是重复提交则抛出异常，交由controllerAdvice处理。如果不是，则正常处理业务逻辑。
- 判断重复提交逻辑：如果客户端重复提交请求，因redis中的key是有5S的生存时间，再次调用setNx命令时会返回false，请求失败，抛出重复提交异常

### 2.2实现原理图

https://www.processon.com/view/link/5e44c630e4b03e660722f43b

## 三、具体代码实现

### 3.1 引入依赖

pom.xml文件

```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>2.0.2.RELEASE</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>com.example</groupId>
	<artifactId>springboot_norepeat</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>springboot_norepeat</name>
	<description>springboot_norepeat for Spring Boot</description>

	<properties>
		<java.version>1.8</java.version>
	</properties>

	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-redis</artifactId>
			<exclusions>
				<exclusion>
					<groupId>io.lettuce</groupId>
					<artifactId>lettuce-core</artifactId>
				</exclusion>
			</exclusions>
		</dependency>

		<dependency>
			<groupId>redis.clients</groupId>
			<artifactId>jedis</artifactId>
			<version>2.9.1</version>
		</dependency>

		<dependency>
			<groupId>cn.hutool</groupId>
			<artifactId>hutool-all</artifactId>
			<version>5.1.3</version>
		</dependency>
		<dependency>
			<groupId>org.aspectj</groupId>
			<artifactId>aspectjweaver</artifactId>
			<version>1.8.9</version>
		</dependency>
		<dependency>
			<groupId>org.apache.commons</groupId>
			<artifactId>commons-pool2</artifactId>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>

	<repositories>
		<repository>
			<id>spring-milestones</id>
			<name>Spring Milestones</name>
			<url>https://repo.spring.io/milestone</url>
		</repository>
	</repositories>
	<pluginRepositories>
		<pluginRepository>
			<id>spring-milestones</id>
			<name>Spring Milestones</name>
			<url>https://repo.spring.io/milestone</url>
		</pluginRepository>
	</pluginRepositories>

</project>
```

### 3.2 keytUtil工具类

```
package com.example.norepeat;

import cn.hutool.json.JSONUtil;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
 
public class KeyUtil {
    public static String generate(Method method, Object... args) throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder(method.toString());
		for (Object arg : args) {
			sb.append(toString(arg));
		}
		return md5(sb.toString());
	}
 
	private static String toString(Object object) {
		if (object == null) {
			return "null";
		}
		if (object instanceof Number) {
			return object.toString();
		}
		return JSONUtil.toJsonStr(object);
	}
 
	public static String md5(String str) {
		StringBuilder buf = new StringBuilder();
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(str.getBytes());
			byte b[] = md.digest();
			int i;
			for (int offset = 0; offset < b.length; offset++) {
				i = b[offset];
				if (i < 0)
					i += 256;
				if (i < 16)
					buf.append(0);
				buf.append(Integer.toHexString(i));
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return buf.toString();
	}
}
```



### 3.3 Norepeat注解

```
package com.example.norepeat;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
 
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Norepeat {
	// redis的key值一部分
	String value();

	// 过期时间
	long expireMillis();
}
```

### 3.4 NorepeatAspect拦截器

```
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
```

### 3.5 redis工具类

```
package com.example.norepeat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCommands;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * redis工具类
 */
@Component
public class RedisService {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 写入缓存
     * @param key
     * @param value
     * @return
     */
    public boolean set(final String key, Object value) {
        boolean result = false;
        try {
            ValueOperations<Serializable, Object> operations = redisTemplate.opsForValue();
            operations.set(key, value);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * setNx写入缓存设置时效时间
     * @param key
     * @param value
     * @return
     */
    public boolean setNx(final String key, String value,long expireTime) {
        return redisTemplate.execute(
                (RedisCallback<String>) conn -> (
                        (JedisCommands) conn.getNativeConnection()).set(key, value,"NX", "PX", expireTime
                )
        ).equals("OK");
    }

    /**
     * 写入缓存设置时效时间
     * @param key
     * @param value
     * @return
     */
    public boolean setEx(final String key, Object value, Long expireTime) {
        boolean result = false;
        try {
            ValueOperations<Serializable, Object> operations = redisTemplate.opsForValue();
            operations.set(key, value);
            redisTemplate.expire(key, expireTime, TimeUnit.SECONDS);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    /**
     * 判断缓存中是否有对应的value
     * @param key
     * @return
     */
    public boolean exists(final String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 读取缓存
     * @param key
     * @return
     */
    public Object get(final String key) {
        Object result = null;
        ValueOperations<Serializable, Object> operations = redisTemplate.opsForValue();
        result = operations.get(key);
        return result;
    }

    /**
     * 删除对应的value
     * @param key
     */
    public boolean remove(final String key) {
        if (exists(key)) {
            Boolean delete = redisTemplate.delete(key);
            return delete;
        }
        return false;

    }

}
```

### 3.6 重复异常类

```
package com.example.norepeat;
public class RepeatException extends RuntimeException {
	public RepeatException(String message) {
		super(message);
	}
	
	@Override
	public String getMessage() {
		return super.getMessage();
	}
}
```

### 3.7 测试实体类

```
package com.example.norepeat;

public class User {
    private String userName;
    private String userAge;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAge() {
        return userAge;
    }

    public void setUserAge(String userAge) {
        this.userAge = userAge;
    }
}
```

### 3.8 测试控制器

```
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
```



## 四.请求重复测试

### 4.1  发送测试请求

打开postman

```
输入请求地址：http://localhost:8080/repeat
设置请求体为json格式：
传入json参数：{"userName":"chenjinhua","userAge":"23"}
```

输出结果

```
redis access ok:chenjinhua 23
```

然后再连续点击发送请求会出现，重复的异常提示

```
{
    "timestamp": "2020-02-13T03:29:04.382+0000",
    "status": 500,
    "error": "Internal Server Error",
    "message": "No message available",
    "path": "/repeat"
}
```

