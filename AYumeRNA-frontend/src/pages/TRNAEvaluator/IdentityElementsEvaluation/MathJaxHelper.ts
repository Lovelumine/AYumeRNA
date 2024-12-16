import { mathjax } from 'mathjax-full/js/mathjax.js'
import { TeX } from 'mathjax-full/js/input/tex.js'
import { CHTML } from 'mathjax-full/js/output/chtml.js'
import { liteAdaptor } from 'mathjax-full/js/adaptors/liteAdaptor.js'
import { RegisterHTMLHandler } from 'mathjax-full/js/handlers/html.js'

const adaptor = liteAdaptor()
RegisterHTMLHandler(adaptor)

const tex = new TeX({ packages: ['base', 'ams'] })
const chtml = new CHTML({
  fontURL: 'https://cdn.jsdelivr.net/npm/mathjax@3/es5/output/chtml/fonts',
})

const html = mathjax.document('', {
  InputJax: tex,
  OutputJax: chtml,
})

/**
 * Render a mathematical formula as HTML using MathJax.
 * @param {string} math - The LaTeX string to render.
 * @returns {Promise<string>} - The rendered HTML as a string.
 */
export async function renderMath(math: string): Promise<string> {
  const node = html.convert(math, { display: true })
  return adaptor.outerHTML(node)
}
