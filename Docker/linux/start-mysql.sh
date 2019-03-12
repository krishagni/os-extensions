#!/bin/bash

print_help() {
  pgm=`basename $0`
  echo "Command: $pgm --help --config-template --run --config=<config-properties> --name=<container-name>";
  echo "--help: prints this help message";
  echo "--config-template: creates database.properties template file";
  echo "--run: Runs MySQL 5.7 container";
  echo "--config: absolute path of database.properties file";
  echo "--name: name of OpenSpecimen instance";
  echo "--version: OpenSpecimen version to run";
}

create_template() {
  if [ -f "database.properties" ]; then
    echo "File database.properties is already present in the current directory."
    return;
  fi

  echo "Creating a database.properties file template."
  echo "#Root password of MySQL" >> database.properties
  echo "MYSQL_ROOT_PASSWORD=" >> database.properties
  echo -e "\n#Database name." >> database.properties
  echo "MYSQL_DATABASE=" >> database.properties
  echo -e "\n#Database user to use for connecting to the database server." >> database.properties
  echo "MYSQL_USER=" >> database.properties
  echo -e "\n#Database user password." >> database.properties
  echo "MYSQL_PASSWORD=" >> database.properties
  echo -e "\n#MySQL data directory" >> database.properties
  echo "mysql_data_dir=/opt/openspecimen/mysql-data" >> database.properties
  echo -e "\n#MySQL database backup directory." >> database.properties
  echo "mysql_backup_dir=/opt/openspecimen/mysql-database-backup" >> database.properties
}

check_db_props() {
  root_password=$(grep "^MYSQL_ROOT_PASSWORD=" $config_file | cut -d'=' -f2 | sed 's/ //g');
  if [ -z "$root_password" ]; then
    echo "Error: Root user password not specified.";
    exit 127;
  fi

  database_name=$(grep "^MYSQL_DATABASE=" $config_file | cut -d'=' -f2 | sed 's/ //g');
  if [ -z "$database_name" ]; then
    echo "Error: Database name not specified.";
    exit 127;
  fi

  database_user=$(grep "^MYSQL_USER=" $config_file | cut -d'=' -f2 | sed 's/ //g');
  if [ -z "$database_user" ]; then
    echo "Error: Database username not specified.";
    exit 127;
  fi

  user_password=$(grep "^MYSQL_PASSWORD=" $config_file | cut -d'=' -f2 | sed 's/ //g');
  if [ -z "$user_password" ]; then
    echo "Error: Database user password not specified.";
    exit 127;
  fi

  mysql_data_dir=$(grep "^mysql_data_dir=" $config_file | cut -d'=' -f2 | sed 's/ //g');
  if [ -z "$mysql_data_dir" ]; then
    echo "Error: MySQL data directory is not specified by default value : /opt/openspecimen/mysql-data ";
    exit 127;
  fi

  mysql_backup_dir=$(grep "^mysql_backup_dir=" $config_file | cut -d'=' -f2 | sed 's/ //g');
  if [ -z "$mysql_backup_dir" ]; then
    echo "Error: Database backup directory is not specified by default value : /opt/openspecimen/mysql-database-backup";
    exit 127;
  fi	
}

run_mysql() {
  if [ -z "$config_file" ]; then
    echo "Error: database.properties file (--config) is not specified. Exiting.";
    exit 127;
  fi

  if [ ! -f "$config_file" ]; then
    echo "Error: Invalid database.properties file: $config_file";
    exit 127;
  fi

  if [ -z "$name" ]; then
    name="openspecimen-mysql";
  fi
  
  check_db_props;
  docker stop $name
  docker rm $name

  docker run -d --name=$name \
	  -e MYSQL_ROOT_PASSWORD=$root_password \
	  -e MYSQL_DATABASE=$database_name \
	  -e MYSQL_USER=$database_user \
	  -e MYSQL_PASSWORD=$user_password \
	  -v $mysql_data_dir:/var/lib/mysql \
	  -v /etc/localtime:/etc/localtime \
	  -v /etc/timezone:/etc/timezone \
	  -v $mysql_backup_dir:/opt/openspecimen/nightly-db-backup \
	krishagni/openspecimen:os-mysql5.7
}

main() {
for i in "$@"
  do
    case $i in
      --help)
        print_help=1
        shift
        ;;
      --config-template)
        cfg_tmpl=1
        shift
        ;;
      --run)
        run_os=1
        shift
        ;;
      --config=*)
        config_file="${i#*=}"
        shift
        ;;
      --name=*)
        name="${i#*=}"
        shift
        ;;
    esac
  done

  if [ "$print_help" == "1" ]; then
    print_help;
  elif [ "$cfg_tmpl" == "1" ]; then
    create_template;
    exit 0;
  elif [ "$run_os" == "1" ]; then
    run_mysql;
  else 
    print_help;
  fi	
}

main $@;
