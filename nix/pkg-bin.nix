{
  lib,
  stdenv,
  fetchzip,
  jdk17,
  unzip,
  bash,
}: let
  meta = (import ./meta.nix) lib;
  version = meta.latest-release;
in
  stdenv.mkDerivation {
    inherit version;
    name = "fit4s-bin-${version}";

    src = fetchzip {
      url = "https://github.com/eikek/fit4s/releases/download/v${version}/fit4s-cli-${version}.zip";
      sha256 = "sha256-b9sJSIgh7C1nzNYDItxKePwJuYEDQ+bYw3oeb8qpSy0=";
    };

    buildPhase = "true";

    installPhase = ''
      mkdir -p $out/{bin,fit4s-${version}}
      cp -R * $out/fit4s-${version}/
      cat > $out/bin/fit4s <<-EOF
      #!${bash}/bin/bash
      $out/fit4s-${version}/bin/fit4s-cli -java-home ${jdk17} "\$@"
      EOF
      chmod 755 $out/bin/fit4s
    '';

    meta = meta.meta-bin;
  }
