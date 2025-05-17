#!/usr/bin/env bash

##############################################
#
# Script converting .ipspolicycmpttype files to plantUml
#
# Prerequisites:
#   xsltproc is installed 
##############################################

#set -x
set -e

PARENT_PATH=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P)

WORKDIR="$PARENT_PATH/workdir"
MODEL_TYPE="policy"
FILE_SUFFIX="ipspolicycmpttype"
PRODUCT_FILE_SUFFIX="ipsproductcmpttype"
OUTPUT_DIR="$PARENT_PATH/output"
PUML_RESULT_FILE="$OUTPUT_DIR/ips-models.puml"
OPTIONS=""
LENGTH=2

usage() {
    cat <<EOT

$0 - Script creating plantUml class diagrams from Faktor-IPS model classes

Usage: $0 [OPTIONS]

Options:
  -o, --output               Output file path (should have .puml suffix). Default: $PUML_RESULT_FILE
  -p, --paths                Path(s) to model directories.
                             For multiple paths: put them in double quotes and separated by space. Ex.: -p "one/path other/path".
                             If no paths are given, we will work on whatever is in the workdir.
  -w, --workdir              Path to working directory with collected ips files. Default: $WORKDIR
  -dw, --delete-workdir      Delete workdir before incorporating new paths (only applicable together with -p).
  -k, --packages             Puts all classes in their packages
  -l, --connector-length     Length of association connectors. Default: $LENGTH
  -r, --print-target-role    Print the targetRolePlural attribute on the composition arrow.
  -s, --add-super-type       Adds inheritance of super types that are NOT present under the scanned models.
  -a, --add-associations     Adds associations to classes that are NOT present under the scanned models.
  -pf, --package-filter      Filter the diagram to a package and it's associations
  -t, --show-tables          Show tables
  -tu, --show-table-usage    Show table usage by product component types (including external tables)
  -et, --show-enum-types     Show enum types
  -ea, --show-enum-assoc     Show enum associations (including external enums)
  -pr, --show-product        Show product components
  -h, --help                 Show this help
EOT
    exit 3
}


args() {
  while [[ "$1" != "" ]]; do
    case $1 in
      -o|--output )                 shift
                                    PUML_RESULT_FILE="$1"
                                    shift
                                    ;;
      -p|--paths-to-dir )           shift
                                    PATHS_TO_DIR="$1"
                                    shift
                                    ;;
      -l|--connector-length )       shift
                                    LENGTH=$1
                                    shift
                                    ;;
      -r|--print-target-role )      OPTIONS="$OPTIONS --stringparam printTargetRole true"
                                    shift
                                    ;;
      -s|--add-super-type )         OPTIONS="$OPTIONS --stringparam addSuperType true"
                                    shift
                                    ;;
      -a|--add-associations )       OPTIONS="$OPTIONS --stringparam addAssociations true"
                                    shift
                                    ;;
      -t|--show-tables )            OPTIONS="$OPTIONS --stringparam showTables true"
                                    shift
                                    ;;
      -tu|--show-table-usage )      OPTIONS="$OPTIONS --stringparam showTableUsage true"
                                    shift
                                    ;;
      -et|--show-enum-types )       OPTIONS="$OPTIONS --stringparam showEnumTypes true"
                                    shift
                                    ;;
      -ea|--show-enum-assoc )       OPTIONS="$OPTIONS --stringparam showEnumAssociations true"
                                    shift
                                    ;;
      -pf| --package-filter )       shift
                                    OPTIONS="$OPTIONS --stringparam packageFilter $1"
                                    shift
                                    ;;
      -pr|--show-products )         OPTIONS="$OPTIONS --stringparam showProductComponents true"
                                    shift
                                    ;;
      -k|--packages )               OPTIONS="$OPTIONS --stringparam packages true"
                                    shift
                                    ;;
      -w|--workdir )                shift
                                    WORKDIR=$1
                                    shift
                                    ;;
      -dw|--delete-workdir )        DELETE_WORKDIR="true"
                                    shift
                                    ;;
      -h|--help )                   usage
                                    ;;
      -*)                           echo "Unrecognized option $1"
                                    usage
                                    ;;
      *)                            echo "Unrecognized parameter $1"
                                    usage
                                    ;;
    esac
  done

  for i in $(seq 1 $LENGTH); do
    CONNECTOR="${CONNECTOR}-"
    DOTTED_CONNECTOR="${DOTTED_CONNECTOR}."
  done

  MODEL_DIR="$WORKDIR/model"
  COLLECTION_XML="$WORKDIR/collection.xml"

  OPTIONS="$OPTIONS --stringparam connector $CONNECTOR"
  OPTIONS="$OPTIONS --stringparam dottedConnector $DOTTED_CONNECTOR"
  mkdir -p $OUTPUT_DIR
}

retrieve_files() {
  echo "copying model files..."
  if [[ "$DELETE_WORKDIR" == "true" ]]; then
    rm -rf $WORKDIR
  fi
  mkdir -p $MODEL_DIR

  for path in $PATHS_TO_DIR; do
    clean_path=$(echo "$path" | sed 's|/$||')
    echo "copying $clean_path to $MODEL_DIR"
    cp -r ${clean_path}/* $MODEL_DIR
  done
}

create_collection() {
  echo "creating collection xml..."
  touch $COLLECTION_XML
  echo '<?xml version="1.0" encoding="UTF-8"?>' > $COLLECTION_XML
  echo "<collection>" >> $COLLECTION_XML

  scan_modeldir_rec $MODEL_DIR
  echo "</collection>" >> $COLLECTION_XML
}

scan_modeldir_rec() {
  local basedir=$1
  local subdir=$2
  local prefix=$3

  echo "Basedir: $basedir"
  echo "Subdir: $subdir"
  echo "Prefix: $prefix"

  for file in $basedir/$subdir/*; do
    filename=${file##*/}
    if [[ -d $file ]]; then
      scan_modeldir_rec "$basedir" "$subdir/$filename" "$prefix$filename."
    else
      if [[ "${filename##*.}" =~ ^ips.* ]]; then
        class="${prefix}${filename%*.*}"
        echo "Classname: $class"
        cat $file \
          | grep -v "<?xml" \
          |  sed 's/xmlns:xsi=".*"//' \
          |  sed 's|xmlns="http://www.faktorzehn.org"||' \
          |  sed "s/<PolicyCmptType\(.*\)>/<PolicyCmptType className=\"$class\"\1>/" \
          |  sed "s/<ProductCmptType2\(.*\)>/<ProductCmptType2 className=\"$class\"\1>/" \
          |  sed "s/<EnumType\(.*\)>/<EnumType className=\"$class\"\1>/" \
          |  sed "s/<TableStructure \(.*\)>/<TableStructure className=\"$class\" \1>/" \
          >> $COLLECTION_XML
      else
        echo "Skipping $filename"
      fi
    fi
  done
}

execute_xslt() {
  echo "executing xslt..."
  xsltproc $OPTIONS ${PARENT_PATH}/ips2plant.xsl $COLLECTION_XML >> $PUML_RESULT_FILE
}

start_puml() {
  echo "@startuml" > $PUML_RESULT_FILE
  echo "'Created with: $0 $@'" >> $PUML_RESULT_FILE
  echo "hide empty members" >> $PUML_RESULT_FILE
}

end_puml() {
  echo "@enduml" >> $PUML_RESULT_FILE
}

main() {
  args "$@"

  if [[ "$PATHS_TO_DIR" != "" ]]; then
    retrieve_files
    create_collection
  fi

  start_puml "$@"

  execute_xslt

  end_puml
}

main "$@"
