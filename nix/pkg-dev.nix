{
  pkgs,
  lib,
  sbt,
}: let
  meta = (import ./meta.nix) lib;
in
  sbt.lib.mkSbtDerivation {
    inherit pkgs;
    inherit (meta) version;

    pname = "fit4s-cli";

    nativeBuildInputs = [pkgs.nodejs pkgs.bash pkgs.sbt];

    # depsWarmupCommand = ''
    #   sbt compile webviewJS/tzdbCodeGen
    # '';

    src = lib.sourceByRegex ../. [
      "^build.sbt$"
      "^modules$"
      "^modules/.*$"
      "^project$"
      "^project/.*$"
      "^make-cli.sh$"
    ];

    depsSha256 = "sha256-2D5i9eMOXbxqpSz4SK1h7GcW2cV0dcsXkfUJPziFdC4=";

    buildPhase = ''
      env
      ls -lha
      bash make-cli.sh
    '';

    installPhase = ''
      mkdir -p $out
      cp -R modules/cli/target/universal/stage/* $out/

      cat > $out/bin/fit4s <<-EOF
      #!${pkgs.bash}/bin/bash
      $out/bin/fit4s-cli -java-home ${pkgs.openjdk} "\$@"
      EOF
      chmod 755 $out/bin/fit4s
    '';
  }
