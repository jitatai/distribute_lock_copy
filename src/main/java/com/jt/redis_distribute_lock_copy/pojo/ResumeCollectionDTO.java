package com.jt.redis_distribute_lock_copy.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeCollectionDTO implements Serializable {
    /**
     * 简历id
     */
    private Long id;
    /**
     * 异步解析id，稍后根据id可获取最终解析结果
     */
    private Long asyncPredictId;
    /**
     * 简历名称
     */
    private String name;
}