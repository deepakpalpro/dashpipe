export function PlaceholderPage({ title }: { title: string }) {
  return (
    <section className="placeholder-page" aria-label={title}>
      <h1>{title}</h1>
      <p>Coming in a later Wave 6 story.</p>
    </section>
  )
}
