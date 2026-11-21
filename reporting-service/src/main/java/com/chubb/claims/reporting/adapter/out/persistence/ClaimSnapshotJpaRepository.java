package com.chubb.claims.reporting.adapter.out.persistence;

import com.chubb.claims.reporting.application.query.OutstandingSummaryRow;
import com.chubb.claims.reporting.domain.model.ClaimStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ClaimSnapshotJpaRepository extends JpaRepository<ClaimSnapshotEntity, UUID> {

    @Query("""
            select new com.chubb.claims.reporting.application.query.OutstandingSummaryRow(
                s.market, s.status, s.claimType, s.currency, count(s), sum(s.claimedAmount))
            from ClaimSnapshotEntity s
            where s.status in :statuses
              and (:market is null or s.market = :market)
            group by s.market, s.status, s.claimType, s.currency
            order by s.market, s.status, s.claimType, s.currency
            """)
    List<OutstandingSummaryRow> summarize(@Param("statuses") Collection<ClaimStatus> statuses,
                                          @Param("market") String market);

    @Query(value = """
            select s from ClaimSnapshotEntity s
            where s.status in :statuses
              and (:market is null or s.market = :market)
              and (:claimType is null or s.claimType = :claimType)
            """,
            countQuery = """
            select count(s) from ClaimSnapshotEntity s
            where s.status in :statuses
              and (:market is null or s.market = :market)
              and (:claimType is null or s.claimType = :claimType)
            """)
    Page<ClaimSnapshotEntity> findOutstanding(@Param("statuses") Collection<ClaimStatus> statuses,
                                              @Param("market") String market,
                                              @Param("claimType") String claimType,
                                              Pageable pageable);
}
