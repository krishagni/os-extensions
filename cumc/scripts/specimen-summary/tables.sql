CREATE TABLE `visit_specimen_type_summary` (
  `IDENTIFIER` bigint NOT NULL AUTO_INCREMENT,
  `VISIT_ID` bigint NOT NULL,
  `SPECIMEN_TYPE` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `COUNT_OF_ALIQUOTS` int DEFAULT NULL,
  `SEQUENCING_TYPE` varchar(255) DEFAULT NULL,
  `SEQUENCING_RESULT` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`IDENTIFIER`),
  KEY `IDX_VSTS_VISIT_ID` (`VISIT_ID`),
  KEY `IDX_VSTS_SPECIMEN_TYPE` (`SPECIMEN_TYPE`),
  KEY `IDX_VSTS_COUNT_OF_ALIQUOTS` (`COUNT_OF_ALIQUOTS`),
  KEY `IDX_VSTS_SEQUENCING_TYPE` (`SEQUENCING_TYPE`),
  KEY `IDX_VSTS_SEQUENCING_RESULT` (`SEQUENCING_RESULT`),
  CONSTRAINT `FK_VSTS_VISIT_ID` FOREIGN KEY (`VISIT_ID`) REFERENCES `catissue_specimen_coll_group` (`IDENTIFIER`)
)

INSERT INTO visit_specimen_type_summary (
        visit_id,
        specimen_type,
        count_of_aliquots,
        sequencing_type,
        sequencing_result
    )
SELECT
    spmn.specimen_collection_group_id AS visit_id,
    spmn_type.value AS specimen_type,
    COUNT(DISTINCT spmn.identifier) AS count_of_aliquots,
    de.DE_A_2 as sequencing_type,
    de.DE_A_5 as sequncing_result
FROM
    catissue_specimen spmn
    LEFT JOIN catissue_coll_event_param coll_event ON coll_event.specimen_id = spmn.identifier
    LEFT JOIN catissue_permissible_value spmn_type ON spmn_type.identifier = spmn.specimen_type_id
    left join (
        OS_SPMN_EXTN_RECS form
        inner join DE_E_11151 de on de.IDENTIFIER = form.RECORD_ID
        and form.FORM_ID = 226
    ) on form.SPECIMEN_ID = spmn.IDENTIFIER
WHERE
    spmn.lineage = 'Aliquot'
    AND spmn.activity_status != 'Disabled'
    AND spmn_type.value IS NOT NULL
    AND spmn.availability_status = 'Available'
    AND spmn.collection_status = 'Collected'
GROUP BY
    spmn.specimen_collection_group_id,
    spmn_type.value,
    sequencing_type,
    sequncing_result

ALTER TABLE visit_specimen_type_summary
ADD COLUMN collection_age INT DEFAULT NULL;

CREATE INDEX IDX_VSTS_COLLECTION_AGE ON visit_specimen_type_summary (collection_age);

SELECT
    vsts.IDENTIFIER AS SUMMARY_ID,
    vsts.VISIT_ID,
    cpr.DOB,
    scg.COLLECTION_TIMESTAMP,
    TIMESTAMPDIFF(YEAR, cpr.DOB, scg.COLLECTION_TIMESTAMP) AS COLLECTION_AGE
FROM visit_specimen_type_summary vsts
JOIN CATISSUE_SPECIMEN_COLL_GROUP scg
  ON vsts.VISIT_ID = scg.IDENTIFIER
JOIN OS_CPR_RECS cpr
  ON scg.COLLECTION_PROTOCOL_REG_ID = cpr.CPR_ID
WHERE scg.ACTIVITY_STATUS != 'Disabled'
  AND cpr.ACTIVITY_STATUS != 'Disabled';


UPDATE visit_specimen_type_summary vsts
JOIN CATISSUE_SPECIMEN_COLL_GROUP scg
  ON vsts.VISIT_ID = scg.IDENTIFIER
JOIN OS_CPR_RECS cpr
  ON scg.COLLECTION_PROTOCOL_REG_ID = cpr.CPR_ID
SET vsts.COLLECTION_AGE = TIMESTAMPDIFF(YEAR, cpr.DOB, scg.COLLECTION_TIMESTAMP)
WHERE scg.ACTIVITY_STATUS != 'Disabled'
  AND cpr.ACTIVITY_STATUS != 'Disabled';

