package com.liyang.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // 限定注解只能作用于类、接口或枚举
@Retention(RetentionPolicy.RUNTIME) // 注解在运行时可用
public @interface Exclude {

}
