{
  description = "fit4s flake";
  inputs = {
    nixpkgs.url = "nixpkgs/nixos-23.11";
    sbt.url = "github:zaninime/sbt-derivation";
    sbt.inputs.nixpkgs.follows = "nixpkgs";
  };

  outputs = { self, nixpkgs, sbt }:
    let
      supportedSystems = [ "x86_64-linux" "aarch64-linux" "x86_64-darwin" ];
      forAllSystems = nixpkgs.lib.genAttrs supportedSystems;
    in
    rec
    {
      overlays.default = final: prev: {
        fit4s = with final; sbt.lib.mkSbtDerivation {
          pkgs = final;

          version = "dynamic";
          pname = "fit4s-cli";

          nativeBuildInputs = [ pkgs.nodejs ];

          # depsWarmupCommand = ''
          #   sbt compile webviewJS/tzdbCodeGen
          # '';

          src = lib.sourceByRegex ./. [
            "^build.sbt$"
            "^modules$"
            "^modules/.*$"
            "^project$"
            "^project/.*$"
            "^make-cli.sh$"
          ];

          depsSha256 = "sha256-+Yj69e4d3o2Nf2kGvK6hmD7fpYyZAqAmoKxC3/10Kmk=";

          buildPhase = ''
            env
            ls -lha
            bash make-cli.sh
          '';

          installPhase = ''
            mkdir -p $out
            cp -R modules/cli/target/universal/stage/* $out/

            cat > $out/bin/fit4s <<-EOF
            #!${bash}/bin/bash
            $out/bin/fit4s-cli -java-home ${jdk} "\$@"
            EOF
            chmod 755 $out/bin/fit4s
          '';
        };
      };

      apps = forAllSystems (system:
        { default = {
            type = "app";
            program = "${self.packages.${system}.default}/bin/fit4s";
          };
        });

      packages = forAllSystems (system:
        {
          default = (import nixpkgs {
            inherit system;
            overlays = [ self.overlays.default ];
          }).fit4s;
        });


      devShells = forAllSystems(system:
        { default =
            let
              overlays = import ./nix/overlays.nix;
              pkgs = import nixpkgs {
                inherit system;
                overlays = [
                  overlays.sbt
                  overlays.postgres-fg
                ];
              };
            in
              pkgs.mkShell {
                buildInputs = [
                  pkgs.sbt
                  pkgs.openjdk
                  pkgs.nodejs
                  pkgs.postgres-fg
                  pkgs.scala-cli
                ];
                nativeBuildInputs =
                  [
                  ];

                SBT_OPTS = "-Xmx2G";
                JAVA_HOME = "${pkgs.openjdk19}/lib/openjdk";
              };
        });
    };
}
