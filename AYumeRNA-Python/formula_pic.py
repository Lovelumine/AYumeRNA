import os
import subprocess

def generate_formula_image_with_latex(formula, output_path, dpi=300):
    """
    Generate a formula image using LaTeX and convert it to a transparent PNG.
    
    :param formula: The LaTeX formula as a string.
    :param output_path: Path to save the PNG image.
    :param dpi: Resolution of the PNG image.
    """
    latex_template = r"""
    \documentclass[preview,border=1pt,varwidth]{standalone}
    \usepackage{amsmath}
    \usepackage{amssymb}
    \begin{document}
    $%s$
    \end{document}
    """
    # Create a temporary LaTeX file
    temp_dir = "temp_latex"
    os.makedirs(temp_dir, exist_ok=True)  # Ensure directory exists
    tex_file = os.path.join(temp_dir, "formula.tex")
    dvi_file = os.path.join(temp_dir, "formula.dvi")
    png_file = os.path.join(temp_dir, "formula.png")

    # Write the LaTeX file
    with open(tex_file, "w") as f:
        f.write(latex_template % formula)

    try:
        # Step 1: Run LaTeX to create DVI
        print(f"Running LaTeX on {tex_file}...")
        subprocess.run(["latex", "formula.tex"], cwd=temp_dir, check=True)
        print("LaTeX processing completed.")

        # Step 2: Convert DVI to PNG
        print(f"Converting {dvi_file} to PNG...")
        subprocess.run(
            ["dvipng", "formula.dvi", "-D", str(dpi), "-T", "tight", "-o", "formula.png", "-bg", "Transparent"],
            cwd=temp_dir,
            check=True
        )
        print("dvipng processing completed.")

        # Move the PNG to the desired output path
        os.rename(png_file, output_path)
        print(f"Saved formula image to {output_path}")
    except subprocess.CalledProcessError as e:
        print(f"Error during LaTeX or dvipng processing: {e}")
    finally:
        # Clean up temporary files
        for file in os.listdir(temp_dir):
            os.remove(os.path.join(temp_dir, file))
        os.rmdir(temp_dir)

# 示例公式
formulas = [
    r"C = \{ \text{set of conserved positions} \}, \, |C| = \text{number of conserved positions}",
    r"s(i) = \begin{cases} +1 & \text{if matches} \\ -1 & \text{if mismatches} \\ -2 & \text{if gap} \end{cases}",
    r"\text{tREX Score} = \frac{\sum_{i \in C} s(i)}{|C|}"
]

# 保存为图片
for i, formula in enumerate(formulas):
    output_file = f"formula_{i+1}.png"
    generate_formula_image_with_latex(formula, output_file)