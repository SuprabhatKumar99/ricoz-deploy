package com.ricozknow.search;

import com.ricozknow.common.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "search_synonyms")
@Getter
@Setter
@NoArgsConstructor
public class SearchSynonym extends TenantOwnedEntity {

    @Column(nullable = false)
    private String term;

    /** JSON array of synonym strings, e.g. ["password reset", "forgot password"]. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String synonyms;
}
