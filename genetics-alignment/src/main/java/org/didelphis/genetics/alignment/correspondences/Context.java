package org.didelphis.genetics.alignment.correspondences;

/**
 * @author Samantha Fiona McCabe
 * Created: 4/10/2016
 */
public class Context<T> {

	private final T ante;
	private final T post;

	public Context(T ante, T post) {
		this.ante = ante;
		this.post = post;
	}

	public T getPost() {
		return post;
	}

	public T getAnte() {
		return ante;
	}

	@Override
	public int hashCode() {
		return 31 * ante.hashCode() + post.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Context<?> context = (Context<?>) o;
		return ante.equals(context.ante) && post.equals(context.post);
	}

	@Override
	public String toString() {
		return "Context{" + "ante=" + ante + ", post=" + post + '}';
	}
}
