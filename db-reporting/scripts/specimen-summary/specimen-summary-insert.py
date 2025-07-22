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
    parser = argparse.ArgumentParser(description='Truncate and insert into visit_specimen_type_summary')
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

def truncate_and_insert(conn):
    truncate_query = "TRUNCATE TABLE visit_specimen_type_summary"
    
    insert_query = """
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
    """
    
    try:
        with conn.cursor() as cursor:
            logging.info("Executing TRUNCATE...")
            cursor.execute(truncate_query)
            logging.info("TRUNCATE complete.")

            logging.info("Executing INSERT...")
            cursor.execute(insert_query)
            conn.commit()
            logging.info("INSERT complete. Records inserted.")
    except mysql.connector.Error as err:
        conn.rollback()
        logging.error(f"Query execution failed: {err}")
    finally:
        conn.close()

if __name__ == "__main__":
    args = parse_args()
    config = load_db_config(args.config_file)
    conn = get_db_connection(config)
    truncate_and_insert(conn)
