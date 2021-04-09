package com.jt.redis_distribute_lock_copy.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeCollectionDO {
    /**
     * 简历id
     */
    private Long id;
    /**
     * 简历名称
     */
    private String name;
}