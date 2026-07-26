{
  lib,
  stdenv,
  fetchurl,
  jre,
  unzip,
  bash,
}: let
  version = "0.14.0";
in
  stdenv.mkDerivation {
    inherit version;
    name = "fit4s-${version}";

    src = fetchurl {
      url = "https://github.com/eikek/fit4s/releases/download/v${version}/fit4s-${version}.zip";
      sha256 = "sha256-J2zHDhI9Q9GcFz5KDEJSKQvtUMNP8ML/Yb6LBdwZ8Iw=";
    };

    buildPhase = "true";
    unpackPhase = ''
      ${unzip}/bin/unzip $src
    '';

    installPhase = ''
      mkdir -p $out/{bin,fit4s-${version}}
      cp -R * $out/fit4s-${version}/
      cat > $out/bin/fit4s <<-EOF
      #!${bash}/bin/bash
      $out/fit4s-${version}/bin/fit4s -java-home ${jre} "\$@"
      EOF
      chmod 755 $out/bin/fit4s
    '';

    meta = {
      description = "fit4s is a scala3 library for reading and writing fit files.";
      license = lib.licenses.asl20;
      homepage = "https://github.com/eikek/fit4s";
    };
  }
