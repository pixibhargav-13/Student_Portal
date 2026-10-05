package com.studentportal.common.repository;

import com.studentportal.common.audit.AuditInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface CoreRepository<T extends AuditInfo, I>
    extends JpaRepository<T, I>, JpaSpecificationExecutor<T> {}
