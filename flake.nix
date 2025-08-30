{
  description = "fit4s flake";
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    devshell-tools.url = "github:eikek/devshell-tools";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
    devshell-tools,
  }:
    flake-utils.lib.eachDefaultSystem (system: let
      pkgs = nixpkgs.legacyPackages.${system};
      ciPkgs = with pkgs; [
        devshell-tools.packages.${system}.mill1_17
      ];
      devshellPkgs =
        ciPkgs
        ++ (with pkgs; [
          jq
          scala-cli
          metals
          (python3.withPackages (p: [p.requests]))
          vscode-langservers-extracted
          typescript-language-server
        ]);
    in {
      formatter = pkgs.alejandra;
      packages = let
        pkgs = import nixpkgs {
          inherit system;
          overlays = [self.overlays.default];
        };
      in {
        default = pkgs.fit4s;
        fit4s = pkgs.fit4s;
      };
      devShells = {
        ci = pkgs.mkShellNoCC {
          buildInputs = ciPkgs;
        };
        default = pkgs.mkShellNoCC {
          buildInputs = ciPkgs ++ devshellPkgs;
        };
      };
    })
    // (let
      cliPkgs = pkgs: rec {
        fit4s = pkgs.callPackage (import ./nix/pkg.nix) {};
      };
    in {
      overlays.default = final: prev: (cliPkgs final);
    });
}
