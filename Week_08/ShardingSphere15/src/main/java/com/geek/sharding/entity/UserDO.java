package com.geek.sharding.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UserDO implements Serializable {
    private Long id;
    private Long consumerId;
    private String userName;
    private String email;
    private String cardNo;
    private String mobile;
    private String state;
    private Integer gender;
    private Date createTime;
    private Date updateTime;
}
