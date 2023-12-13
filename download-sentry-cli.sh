#!/bin/bash
cd $(dirname "$0")

props_file="sentry-cli.properties"

function prop {
  grep "$1" $2 | cut -d'=' -f2 | xargs
}

base_url="$(prop 'repo' $props_file)/releases/download/$(prop 'version' $props_file)"
target_dir="src/main/resources/bin/"
actual_props_file="$target_dir${props_file}"
PLATFORMS="Darwin-universal Linux-i686 Linux-x86_64 Windows-i686 Linux-aarch64"

function shouldDownload {
    if ! cmp -s "$props_file" "$actual_props_file"; then
        echo "Props file differ, going to download cli"
        return
    fi

    if test $(ls $target_dir | wc -l) -lt 5; then
        echo "Sentry cli missing, going to download"
        return
    fi

    echo "Sentry cli exists, not downloading"

    false
}

if shouldDownload; then
    rm -f $target_dir/sentry-cli-*
    for plat in $PLATFORMS; do
      suffix=''
      if [[ $plat == *"Windows"* ]]; then
        suffix='.exe'
      fi
      echo "${plat}"
      download_url=$base_url//sentry-cli-${plat}${suffix}
      fn="$target_dir/sentry-cli-${plat}${suffix}"
      curl -SL --progress-bar "$download_url" -o "$fn"
      chmod +x "$fn"
      cp $props_file $target_dir/$props_file
    done
fi
