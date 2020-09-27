git checkout $1 2> /dev/null
echo -n $1 "$(git log -1 --date=format:'%Y/%m/%d' --format='%ad') "
cloc . | egrep "Kotlin|^C\+\+|Java " | sed 's/[ \t]\+/ /g' | cut -f 1,5 -d ' ' | tr ' ' '=' | tr '\n' ',' | sed 's/,$//'
echo

