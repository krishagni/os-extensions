CREATE TABLE `visit_specimen_type_summary` (
  `IDENTIFIER` bigint NOT NULL AUTO_INCREMENT,
  `VISIT_ID` bigint NOT NULL,
  `SPECIMEN_TYPE` varchar(255) COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `COUNT_OF_ALIQUOTS` int DEFAULT NULL,
  `SPECIMEN_QUANTITY` decimal(24,8) DEFAULT NULL,
  `COLLECTION_CONTAINER` varchar(255) COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `SPECIMENS_COUNT` int DEFAULT NULL,
  `LINEAGE` varchar(255) COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `AVAILABILITY_STATUS` varchar(50) COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`IDENTIFIER`),
  KEY `IDX_VSTS_VISIT_ID` (`VISIT_ID`),
  KEY `IDX_VSTS_SPECIMEN_TYPE` (`SPECIMEN_TYPE`),
  KEY `IDX_VSTS_COUNT_OF_ALIQUOTS` (`COUNT_OF_ALIQUOTS`),
  KEY `IDX_VSTS_SPECIMEN_QUANTITY` (`SPECIMEN_QUANTITY`),
  KEY `IDX_VSTS_COLLECTION_CONTAINER` (`COLLECTION_CONTAINER`),
  KEY `IDX_VSTS_SPECIMENS_COUNT` (`SPECIMENS_COUNT`),
  KEY `IDX_VSTS_LINEAGE` (`LINEAGE`),
  KEY `AVAILABILITY_STATUS` (`AVAILABILITY_STATUS`),
  CONSTRAINT `FK_VSTS_VISIT_ID` FOREIGN KEY (`VISIT_ID`) REFERENCES `catissue_specimen_coll_group` (`IDENTIFIER`)
)

CREATE TABLE `receive_in_lab_summary` (
  `IDENTIFIER` bigint NOT NULL AUTO_INCREMENT,
  `VISIT_ID` bigint NOT NULL,
  `SPECIMEN_TYPE` varchar(255) COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `COUNT_OF_ALIQUOTS` int DEFAULT NULL,
  `SPECIMEN_QUANTITY` decimal(24,8) DEFAULT NULL,
  `COLLECTION_CONTAINER` varchar(255) COLLATE utf8mb3_unicode_ci DEFAULT NULL,
   `RECIEVE_IN_LAB_TIME` varchar(255) COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `SPECIMENS_COUNT` int DEFAULT NULL,
  `LINEAGE` varchar(255) COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `AVAILABILITY_STATUS` varchar(50) COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`IDENTIFIER`),
  KEY `IDX_RILS_VISIT_ID` (`VISIT_ID`),
  KEY `IDX_RILS_SPECIMEN_TYPE` (`SPECIMEN_TYPE`),
  KEY `IDX_RILS_COUNT_OF_ALIQUOTS` (`COUNT_OF_ALIQUOTS`),
  KEY `IDX_RILS_SPECIMEN_QUANTITY` (`SPECIMEN_QUANTITY`),
  KEY `IDX_RILS_COLLECTION_CONTAINER` (`COLLECTION_CONTAINER`),
  KEY `IDX_RILS_SPECIMENS_COUNT` (`SPECIMENS_COUNT`),
  KEY `IDX_RILS_LINEAGE` (`LINEAGE`),
  KEY `IDX_RILS_RECIEVE_IN_LAB_TIME` (`RECIEVE_IN_LAB_TIME`),
  KEY `IDX_RILS_AVAILABILITY_STATUS` (`AVAILABILITY_STATUS`),
  CONSTRAINT `FK_RILS_VISIT_ID` FOREIGN KEY (`VISIT_ID`) REFERENCES `catissue_specimen_coll_group` (`IDENTIFIER`)
);

INSERT INTO visit_specimen_type_summary (
            VISIT_ID,
            SPECIMEN_TYPE,
            COUNT_OF_ALIQUOTS,
            SPECIMEN_QUANTITY,
            COLLECTION_CONTAINER,
            LINEAGE,
            AVAILABILITY_STATUS
        )
        SELECT
            spmn.specimen_collection_group_id AS visit_id,
            spmn_type.value AS specimen_type,
            COUNT(DISTINCT spmn.identifier) AS count_of_aliquots,
            SUM(spmn.available_quantity) AS specimen_quantity,
            coll_container.coll_container AS collection_container,
            spmn.lineage AS lineage,
            spmn.availability_status AS availability_status
        FROM
            catissue_specimen spmn
            LEFT JOIN catissue_coll_event_param coll_event 
                ON coll_event.specimen_id = spmn.identifier
            LEFT JOIN catissue_permissible_value spmn_type 
                ON spmn_type.identifier = spmn.specimen_type_id
            LEFT JOIN OS_SPMN_COLL_RECV_DETAILS_VIEW coll_container 
                ON coll_container.specimen_id = spmn.identifier
        WHERE
            spmn.activity_status != 'Disabled'
            AND spmn_type.value IS NOT NULL
            AND spmn.identifier BETWEEN {start_id} AND {end_id}
        GROUP BY
            spmn.specimen_collection_group_id,
            spmn_type.value,
            coll_container.coll_container,
            spmn.lineage,
            spmn.availability_status


INSERT INTO receive_in_lab_summary (
            VISIT_ID,
            SPECIMEN_TYPE,
            COUNT_OF_ALIQUOTS,
            SPECIMEN_QUANTITY,
            COLLECTION_CONTAINER,
            RECEIVE_IN_LAB_TIME,
            SPECIMENS_COUNT,
            LINEAGE,
            AVAILABILITY_STATUS
        )
        SELECT
            spmn.specimen_collection_group_id AS visit_id,
            spmn_type.value AS specimen_type,
            COUNT(DISTINCT spmn.identifier) AS count_of_aliquots,
            SUM(spmn.available_quantity) AS specimen_quantity,
            coll_container.coll_container AS collection_container,
            DATE(DATE_SUB(de.de_a_5, INTERVAL DAY(de.de_a_5) - 1 DAY)) AS RECEIVE_IN_LAB_DATE,
            COUNT(spmn.identifier) AS specimens_count,
            spmn.lineage AS lineage,
            spmn.availability_status AS availability_status
        FROM
            catissue_specimen spmn
            LEFT JOIN catissue_coll_event_param coll_event
                ON coll_event.specimen_id = spmn.identifier
            LEFT JOIN catissue_permissible_value spmn_type
                ON spmn_type.identifier = spmn.specimen_type_id
            LEFT JOIN OS_SPMN_COLL_RECV_DETAILS_VIEW coll_container
                ON coll_container.specimen_id = spmn.identifier
            LEFT JOIN (
                OS_SPMN_EXTN_RECS ext
                INNER JOIN DE_E_11426 de
                    ON de.IDENTIFIER = ext.RECORD_ID
                    AND ext.FORM_ID = 501
            ) ON ext.SPECIMEN_ID = spmn.IDENTIFIER
        WHERE
            spmn.activity_status != 'Disabled'
            AND spmn_type.value IS NOT NULL
            AND de.de_a_5 IS NOT NULL
            AND spmn.identifier BETWEEN {start_id} AND {end_id}
        GROUP BY
            spmn.specimen_collection_group_id,
            spmn_type.value,
            coll_container.coll_container,
            DATE(DATE_SUB(de.de_a_5, INTERVAL DAY(de.de_a_5) - 1 DAY)),
            spmn.lineage,
            spmn.availability_status
