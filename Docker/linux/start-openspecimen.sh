#!/bin/bash

set +x

print_help() {
  pgm=`basename $0`
  echo "Command: $pgm --help --config-template --run --config=<config-properties> --name=<container-name> --version=<os-version>";
  echo "--help: prints this help message";
  echo "--config-template: creates config properties template file";
  echo "--run: run OpenSpecimen container";
  echo "--config: absolute path of OpenSpecimen config properties file";
  echo "--name: name of OpenSpecimen instance";
  echo "--version: OpenSpecimen version to run";
}

create_template() {
  if [ -f "config.properties" ]; then
    echo "File config.properties is already present in the current directory."
    return;
  fi

  echo "Creating a config.properties file template."
  echo "#IP address of the database server." >> config.properties
  echo "db_host=" >> config.properties
  echo -e "\n#Port no. of the database server. By default, MySQL listens on 3306 and Oracle on 1521 for incoming connections." >> config.properties
  echo "db_port=" >> config.properties 
  echo -e "\n#Database user to use for connecting to the database server." >> config.properties
  echo "db_user=" >> config.properties
  echo -e "\n#Database user password." >> config.properties
  echo "db_passwd=" >> config.properties
  echo -e "\n#Database server type : mysql or oracle." >> config.properties
  echo "db_type=" >> config.properties 
  echo -e "\n#Database name: schema or service name." >> config.properties
  echo "db_name=" >> config.properties 
  echo -e "\n#If database was created by caTissue then specify yes" >> config.properties
  echo "from_catissue=no" >> config.properties 
  echo -e "\n#Absolute path of the directory that will contain the data and log files." >> config.properties
  echo "data_dir=" >> config.properties
  echo -e "\n#AJP connector port" >> config.properties
  echo "ajp_port=8009" >> config.properties
  echo -e "\n#HTTP connector port" >> config.properties
  echo "http_port=8080" >> config.properties
}

check_db_props() {
  db_host=$(grep "^db_host=" $config_file | cut -d'=' -f2 | sed 's/ //g');
  if [ -z "$db_host" ]; then
    echo "Error: Database server hostname/IP not specified.";
    exit 127;
  fi
  
  db_port=$(grep "^db_port=" $config_file | cut -d'=' -f2 | sed 's/ //g');
  if [ -z "$db_port" ]; then
    echo "Error: Database server port not specified.";
    exit 127;
  fi
  
  db_name=$(grep "^db_name=" $config_file | cut -d'=' -f2 | sed 's/ //g');
  if [ -z "$db_name" ]; then
    echo "Error: Database name not specified.";
    exit 127;
  fi
  
  db_user=$(grep "^db_user=" $config_file | cut -d'=' -f2 | sed 's/ //g');
  if [ -z "$db_user" ]; then
    echo "Error: Database username not specified.";
    exit 127;
  fi
  
  db_passwd=$(grep "^db_passwd=" $config_file | cut -d'=' -f2 | sed 's/ //g');
  if [ -z "$db_passwd" ]; then
    echo "Error: Database user password not specified.";
    exit 127;
  fi
  
  db_type=$(grep "^db_type=" $config_file | cut -d'=' -f2 | sed 's/ //g');
  if [ -z "$db_type" ]; then
    echo "Error: Database server type (mysql/oracle) not specified";
    exit 127;
  fi
}
  
run_server() {
  if [ -z "$config_file" ]; then
    echo "Error: OpenSpecimen configuration file (--config) not specified. Exiting.";
    exit 127;
  fi

  if [ ! -f "$config_file" ]; then
    echo "Error: Invalid config file: $config_file";
    exit 127;
  fi

  data_dir=$(grep "^data_dir=" $config_file | cut -d'=' -f2 | sed 's/ //g');
  ajp_port=$(grep "^ajp_port=" $config_file | cut -d'=' -f2 | sed 's/ //g');
  http_port=$(grep "^http_port=" $config_file | cut -d'=' -f2 | sed 's/ //g');

  if [ -z "$data_dir" ]; then
    echo "Error: data_dir not specified. Please specify the value for data_dir in config file.";
    exit 127;
  fi
  
  if [ -z "$version" ]; then
    echo "Error: OpenSpecimen version (--version) is not specified.";
    exit 127;
  fi

  check_db_props;

  if [ -z "$name" ]; then
    name="openspecimen";
  fi

  if [ -z "$ajp_port" ]; then
    ajp_port=8009;
  fi

  if [ -z "$http_port" ]; then
    http_port=8080;
  fi

  user=`id -u`
  group=`id -g`

  docker stop $name
  docker rm $name
  docker login

  sed -i "s/<version>/$version/g" docker-compose.yml
  sed -i "s/<config_file>/$config_file/g" docker-compose.yml
  sed -i "s/<user>/$user/g" docker-compose.yml
  sed -i "s/<group>/$group/g" docker-compose.yml
  sed -i "s/<http_port>/$http_port/g" docker-compose.yml
  sed -i "s/<ajp_port>/$ajp_port/g" docker-compose.yml
  
  docker-compose up -d
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
        --version=*)
        version="${i#*=}"
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
    run_server;
  else 
    print_help;
  fi
}

main $@;
