function setCarouselSlide(index) {
    const carousel = document.getElementById('productCarousel');
    const carouselInstance = bootstrap.Carousel.getOrCreateInstance(carousel);
    carouselInstance.to(index);

    // Update active thumbnail
    document.querySelectorAll('.thumbnail').forEach((thumb, i) => {
        thumb.classList.toggle('active', i === index);
    });
}