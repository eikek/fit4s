{
  description = "fit4s flake";
  inputs = {
    nixpkgs.url = "nixpkgs/nixos-unstable";
    devshell-tools.url = "github:eikek/devshell-tools";
    sbt.url = "github:zaninime/sbt-derivation";
    sbt.inputs.nixpkgs.follows = "nixpkgs";
  };

  outputs = {
    self,
    nixpkgs,
    devshell-tools,
    sbt,
  }: let
    supportedSystems = ["x86_64-linux" "aarch64-linux" "x86_64-darwin"];
    forAllSystems = nixpkgs.lib.genAttrs supportedSystems;

    fit4sPkgs = pkgs: {
      fit4s-dev = import ./nix/pkg-dev.nix {
        inherit pkgs;
        inherit sbt;
        lib = pkgs.lib;
      };
      fit4s = pkgs.callPackage (import ./nix/pkg-bin.nix) {};
    };
  in rec
  {
    overlays.default = final: prev: (fit4sPkgs final);

    formatter = forAllSystems (
      system:
        nixpkgs.legacyPackages.${system}.alejandra
    );

    apps = forAllSystems (system: {
      default = {
        type = "app";
        program = "${self.packages.${system}.default}/bin/fit4s";
      };
    });

    packages = forAllSystems (system: let
      pkgs = import nixpkgs {
        inherit system;
        overlays = [self.overlays.default];
      };
    in {
      default = pkgs.fit4s;
      fit4s = pkgs.fit4s;
      fit4s-dev = pkgs.fit4s-dev;
    });

    devShells = forAllSystems (system: let
      pkgs = import nixpkgs {
        inherit system;
        overlays = [
          devshell-tools.overlays.default
        ];
      };
      ciPkgs = with pkgs; [
        sbt21 nodejs
      ];
    in {
      ci = pkgs.mkShellNoCC {
        buildInputs = ciPkgs;
        SBT_OPTS = "-Xmx3G";
      };
      default = pkgs.mkShellNoCC {
        buildInputs = ciPkgs ++ [
          pkgs.scala-cli
          pkgs.metals
          pkgs.tailwindcss
        ];
        nativeBuildInputs = [
        ];

        SBT_OPTS = "-Xmx2G";
      };
    });
  };
}
