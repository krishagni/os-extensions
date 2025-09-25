#!/usr/bin/env python3

import mysql.connector
import logging
import sys
import argparse
import json

# Configure logging
logging.basicConfig(
    filename='truncate_insert.log',
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)

def parse_args():
    parser = argparse.ArgumentParser(description='Truncate and insert into visit_specimen_type_summary in batches')
    parser.add_argument('config_file', help='Path to DB config JSON file')
    return parser.parse_args()

def load_db_config(config_path):
    try:
        with open(config_path) as f:
            return json.load(f)
    except Exception as e:
        logging.error(f"Failed to load config file: {e}")
        sys.exit(1)

def get_db_connection(config):
    try:
        return mysql.connector.connect(
            host=config['host'],
            user=config['user'],
            password=config['password'],
            database=config['database']
        )
    except mysql.connector.Error as err:
        logging.error(f"Error connecting to DB: {err}")
        sys.exit(1)

def get_max_identifier(conn):
    with conn.cursor() as cursor:
        cursor.execute("SELECT MAX(identifier) FROM catissue_specimen")
        max_id = cursor.fetchone()[0]
        return max_id or 0

def truncate_and_insert_batches(conn, batch_size=100):
    truncate_query = "TRUNCATE TABLE visit_specimen_type_summary"

    insert_query_template = """
       INSERT INTO visit_specimen_type_summary (
    VISIT_ID,
    SPECIMEN_TYPE,
    COUNT_OF_ALIQUOTS,
    SPECIMEN_QUANTITY,
    COLLECTION_CONTAINER,
    LINEAGE,
    AVAILABILITY_STATUS,
    REASON_DESTROYED,
    STORED_STATUS
)
SELECT
    spmn.specimen_collection_group_id AS visit_id,
    spmn_type.value AS specimen_type,
    COUNT(DISTINCT spmn.identifier) AS count_of_aliquots,
    SUM(spmn.available_quantity) AS specimen_quantity,
    coll_container.coll_container AS collection_container,
    spmn.lineage AS lineage,
    spmn.availability_status AS availability_status,
    decust.de_a_12 AS reason_destroyed,
    CASE WHEN pos.container_name IS NOT NULL THEN 'YES' ELSE 'NO' END AS stored_status
FROM
    catissue_specimen spmn
    LEFT JOIN catissue_coll_event_param coll_event 
        ON coll_event.specimen_id = spmn.identifier
    LEFT JOIN catissue_permissible_value spmn_type 
        ON spmn_type.identifier = spmn.specimen_type_id
    LEFT JOIN OS_SPMN_COLL_RECV_DETAILS_VIEW coll_container 
        ON coll_container.specimen_id = spmn.identifier
    LEFT JOIN (
        os_spmn_cust_fields CUST
        INNER JOIN DE_E_11154 decust
            ON decust.IDENTIFIER = CUST.RECORD_ID
           AND CUST.FORM_ID = 226
    ) ON CUST.SPECIMEN_ID = spmn.IDENTIFIER
    LEFT JOIN OS_SPECIMEN_POSITIONS_VIEW pos
        ON pos.specimen_id = spmn.identifier
WHERE
    spmn.activity_status != 'Disabled'
    AND spmn_type.value IS NOT NULL
    AND spmn.identifier BETWEEN {start_id} AND {end_id}
GROUP BY
    spmn.specimen_collection_group_id,
    spmn_type.value,
    coll_container.coll_container,
    spmn.lineage,
    spmn.availability_status,
    decust.de_a_12,
    stored_status;
    """

    try:
        with conn.cursor() as cursor:
            logging.info("Executing TRUNCATE...")
            cursor.execute(truncate_query)
            conn.commit()
            logging.info("TRUNCATE complete.")

            max_id = get_max_identifier(conn)
            start_id = 1

            while start_id <= max_id:
                end_id = start_id + batch_size - 1
                logging.info(f"Inserting batch: {start_id} to {end_id}")

                insert_query = insert_query_template.format(
                    start_id=start_id,
                    end_id=end_id
                )
                cursor.execute(insert_query)
                conn.commit()

                start_id += batch_size

            logging.info("All batches inserted successfully.")
    except mysql.connector.Error as err:
        conn.rollback()
        logging.error(f"Query execution failed: {err}")
    finally:
        conn.close()

if __name__ == "__main__":
    args = parse_args()
    config = load_db_config(args.config_file)
    conn = get_db_connection(config)
    truncate_and_insert_batches(conn, batch_size=100)
