package com.nowcoder.community.service;

import com.nowcoder.community.dao.AlphaDao;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
// @Scope("singleton") default
// @Scope("prototype") non-singleton
public class AlphaService {

    @Autowired
    private AlphaDao alphaDao;

    public AlphaService(){
        System.out.println("Construction");
    }

    @PostConstruct
    public void init() {
        System.out.println("Initializing alpha service");
    }

    @PreDestroy
    public void destroy(){
        System.out.println("Destroying alpha service");
    }

    public String find(){
        return alphaDao.select();
    }
}
