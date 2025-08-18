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
        devshell-tools.packages.${system}.mill17
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
      devShells = {
        ci = pkgs.mkShellNoCC {
          buildInputs = ciPkgs;
        };
        default = pkgs.mkShellNoCC {
          buildInputs = ciPkgs ++ devshellPkgs;
        };
      };
    });
}
